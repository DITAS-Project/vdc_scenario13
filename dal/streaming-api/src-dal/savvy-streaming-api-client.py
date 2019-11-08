from __future__ import print_function
import logging

import grpc

import savvy_streaming_api_pb2
import savvy_streaming_api_pb2_grpc


def run():

    with grpc.insecure_channel('localhost:40001') as channel:

        # Fake an Authorization header with a JWT token
        #metadata = [('authorization', 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImF1eXRjMlRfYy03WTlIUS1UQjVRQk1za1B2WENZelhTZko2ck9pQkt2LUUifQ.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJpc3MiOiJodHRwczovLzE1My45Mi4zMC41Njo1ODA4MC9hdXRoL3JlYWxtcy8yODgifQ.OUUD-yrtMiS4yv2d5_k6HbRAPugtDoGb02W85OG_p-s')]

        stub = savvy_streaming_api_pb2_grpc.SavvyStreamingAPIStub(channel)
        response = stub.StreamMachine(savvy_streaming_api_pb2.StreamParameters(machineId='CMX_LQLS26'))
        #response = stub.StreamMachine(request=savvy_streaming_api_pb2.StreamParameters(machineId='CMX_LQLS26'), metadata=metadata)

        for line in response:
            print (line.responseLine)
        #print("La API de Savvy ha respondido: " + response.responseLine)

if __name__ == '__main__':
    #logging.basicConfig()

    try:
        run()
    except KeyboardInterrupt:
        print('BYE')
