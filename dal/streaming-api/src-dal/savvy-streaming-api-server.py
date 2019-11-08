# -*- coding: utf-8 -*-
from concurrent import futures
import time
import logging
import json
import requests
import sys
import traceback
import grpc
import jwt

import configparser
from helpers.config import Config

import savvy_streaming_api_pb2
import savvy_streaming_api_pb2_grpc

# RSA key decruption from here down
import argparse
import base64
import six
import struct
import json
from cryptography.hazmat.primitives.asymmetric.rsa import RSAPublicNumbers
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization

# Whether the test mode is on (don't check for authorization)
testing = False

class SavvyStreamingAPI(savvy_streaming_api_pb2_grpc.SavvyStreamingAPIServicer):

    ######
    # METHODS FOR RSA KEY DECRYPTION
    #####
    def int_arr_to_long(self, arr):
        return int(''.join(["%02x" % byte for byte in arr]), 16)

    def base64_to_long(self, data):
        if isinstance(data, six.text_type):
            data = data.encode("ascii")
        _d = base64.urlsafe_b64decode(bytes(data) + b'==')
        return self.int_arr_to_long(struct.unpack('%sB' % len(_d), _d))

    def get_PEM_from_RSA(self, modulus, exponent):
        exponent_long = self.base64_to_long(exponent)
        modulus_long = self.base64_to_long(modulus)
        numbers = RSAPublicNumbers(exponent_long, modulus_long)
        public_key = numbers.public_key(backend=default_backend())
        pem = public_key.public_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PublicFormat.SubjectPublicKeyInfo
        )
        return pem
    #####
    #  END OF METHODS FOR RSA KEY DECRYPTION
    #####

    def JWT_is_valid(self, request, context, accepted_roles):
        """ Check if the JWT token (which is taken from the "auhorization" header) token is valid for an accepted role
            This is a implementation of the "Token validation" section of the ""[DITAS] VDC Access Control" document

            Returns:
                bool(True): If the token is valid
                bool(False): If the token is not valid
        """

        print("Performing JWT check")

        # Don't check for auth in testing mode
        if testing:
            print("DAL is in testing mode, omitting auth check")
            return True

        # The JWT token comes from {"authorization": "Bearer xxxx.yyyyy.zzzz"}, so we only get the last part ("xxxx.yyyyy.zzzz")
        try:
            # Get the metadata
            metadata = dict(context.invocation_metadata())
            #auth_header = metadata["authorization"]
            auth_header = request.authorization
            jwt_token = auth_header.split(" ")[1]
            print ("Token: " + jwt_token);
        except KeyError as ke:
            print("Can't find authorization header: " + str(ke))
            return False
        except IndexError as ie:
            print("Found authorization with unexpected format: " + auth_header)
            print ("IndexError: " + str(ie))
            print("Expected: Bearer here.the.token")
            return False
        except Exception as e:
            print("Failed to retrieve or parse authorization header: " + type(e).__name__ + " " + str(e))
            #traceback.print_exc()
            return False

        # Check if it has three parts separated by dots
        if len(jwt_token.split(".")) != 3:
            print("Error validating JWT token, the token length must be 3 after splitting by .2")
            return False

        print("The token preliminary checks are ok, checking real content")

        try:
            # Get the key_id from the header of the JWT
            key_id = jwt.get_unverified_header(jwt_token)["kid"]
            print('kid from token: ' + str(key_id))

            # Get the algorithm from the header of the JWT
            algorithm = jwt.get_unverified_header(jwt_token)["alg"]
            print('Algorithm from token: ' + str(algorithm))

            # Decode without verification in order to get the payload from the token
            unverified_payload = jwt.decode(jwt_token, verify=False)

            # Get the audience in order to verify the token afterwards
            audience = unverified_payload["aud"]

            # Generate the URL to get the public key
            available_keys_url = unverified_payload["iss"] + "/protocol/openid-connect/certs"

            # Get the keys from the keycloak server
            r = requests.get(available_keys_url, verify=False)
            available_keys_from_keycloak = json.loads(r.text)
            print('Available keys from keycloak: ' + str(available_keys_from_keycloak))
        except Exception as e:
            print ("Exception processing the token " + str(e))
            traceback.print_exc()
            return False

        # For each key of the keycloak server
        for key_of_keycloak in available_keys_from_keycloak["keys"]:
            # Check if it matches the kid got from the JWT header
            if key_of_keycloak["kid"] == key_id:
                # Found key!
                print("Found key: " + key_id)

                try:
                    # The key comes in modulus exponent format
                    # https://stackoverflow.com/questions/39890232/how-to-decode-keys-from-keycloak-openid-connect-cert-api
                    pub_key = self.get_PEM_from_RSA(key_of_keycloak["n"], key_of_keycloak["e"])
                    # Bytes to string
                    pub_key = pub_key.decode('utf-8')
                    print('The public key in plain text is: ' + pub_key)

                    # Decode the JWT with the provided secret
                    decoded_jwt_payload = jwt.decode(jwt_token, pub_key, audience=audience, algorithms=[algorithm])

                    # Check if any user role (taken from the payload) is an accepted role
                    for accepted_role in decoded_jwt_payload["realm_access"]["roles"]:
                        if accepted_role in accepted_roles:
                            # He has access for this method!
                            print('Found role ' + accepted_role + '. Role validation ok')
                            return True
                except Exception as e:
                    print ("Exception decoding the token " + str(e))
                    traceback.print_exc()
                    return False
        # Key not found on the keycloak server
        print('Key not found in available keys in keycloak server')
        return False


    def StreamMachine(self, request, context):
        print('StreamMachine called')
        
        # Callback to detect client disconnections
        # TODO test this with BloomRPC client as grpc node-red node doesn't seem to handle disconnections gracefully
        #context.add_callback(stop_stream)
        
        # Acepted roles for this function - This roles should match the ones created on Keycloak
        accepted_roles = ["ideko-operator", "spart-operator"]
        
        # Check if the JWT token is valid before doing anything
        if self.JWT_is_valid(request, context, accepted_roles):
            machineId = request.machineId

            # TODO gestionar errores, grpc tiene una forma propia
            try:
                machine_box_ip = Config.read('MACHINES', machineId)
            except configparser.NoOptionError as e:
                context.abort(grpc.StatusCode.NOT_FOUND, 'Te given machine id doesn\'t exist')
                return

            print('Requested machine: ' + machineId)
            print('Box IP from config: ' + machine_box_ip)

            port = '7888'
            url_stream = 'http://' + machine_box_ip + ':' + port + '/stream?machines=' + machineId

            print('URL to be called: ' + url_stream)

            result_requests = requests.get(url_stream, stream = True, timeout = 15)
            for line in result_requests.iter_lines():
                # Remember The first line of the stream is only the status
                #if raw_result.find("status") < 0:
                
                # Revisar context.is_active() aunque pareceq ue recobimos un ServerContext y esto es SharedContext.
                # https://grpc.github.io/grpc/python/grpc.html#grpc.RpcContext.is_active
                # Check if connection still alive
                if not context.is_active():
                    # TODO does this free the thread servicer?
                    print("Context is not active. Client disconnected, shutting down this call.")
                    # This may make no sense as we have no client
                    context.abort(grpc.StatusCode.CANCELLED, 'Context not active, client gone')
                    return
                
                print('[Machine ' + machineId + '] Sending response line (first 100 chars): ' +  str(line)[0:100])
                yield savvy_streaming_api_pb2.StreamResponse(responseLine=line)
        else:
            # Unauthorized, the JWT toke does not validate
            context.abort(grpc.StatusCode.PERMISSION_DENIED, 'The JWT token does not validate')
            return
            context.set_code(grpc.StatusCode.PERMISSION_DENIED)
            context.set_details('The JWT token does not validate')
            # Revisar: https://grpc.github.io/grpc/python/grpc.html#service-side-context
            yield savvy_streaming_api_pb2.StreamResponse()

def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    savvy_streaming_api_pb2_grpc.add_SavvyStreamingAPIServicer_to_server(SavvyStreamingAPI(), server)
    server.add_insecure_port('[::]:40001')
    server.start()
    print('Server started on port 40001')
    
    try:
        while True:
            time.sleep(3600)
    finally:
        print('Stopping')
        event = server.stop(grace=0)
        print('Waiting stop: %s', event)
        event.wait()
        print('Stopped')

if __name__ == '__main__':
    #logging.basicConfig()

    # Check for a --testing arg
    if '--testing' in sys.argv:
        print ('DAL started in test mode');
        testing = True
    else:
        print ('Dal is no in test mode, auth checks will be performed')

    serve()
