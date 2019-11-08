FROM ubuntu:18.04

RUN apt-get update

# Install Python3 and curl
RUN apt-get install -y python3
RUN apt-get install -y curl

# Intall distutils and related packages
RUN apt-get install -y python3-distutils python3-setuptools python3-distutils-extra python3-distlib

# Install pip 
RUN curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py
RUN python3 get-pip.py

# grpc and other dependencies
RUN python3 -m pip install grpcio
RUN python3 -m pip install grpcio-tools
RUN python3 -m pip install requests
RUN python3 -m pip uninstall jwt
RUN python3 -m pip uninstall PyJWT
RUN python3 -m pip install PyJWT
RUN python3 -m pip install cryptography

# Bring DAL code into image
WORKDIR /opt/dal
COPY ./src-dal .

EXPOSE 40001

# Execute the server 
CMD ["python3", "-u", "/opt/dal/savvy-streaming-api-server.py"]
