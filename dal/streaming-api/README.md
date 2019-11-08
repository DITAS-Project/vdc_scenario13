# DAL Savvy Streaming API

DAL for Savvy Streaming API, the API right from the box. Expose a single method and uses `grpc`. 

* The source code is in `src-dal`. It can be tested from there in an isolated way.
* There is also a Dockerfile to containerize the DAL.

# Requirements

* Python 3.6+ (Tested the compatibility with Python 3.6 and 3.7)
* Libraries:

  * request
  * grpcio
  * grpcio-tools
  
* The server muct run in the same local network as the Savvy Boxes, otherwise the API coulnd't be reached.

# Methods

This DAl exposes just one method: `StreamMachine`. Take a look at the files in _proto_ folder for more details.

# One or three DALs?

Remenber the use case turns around an scenario where there are three machines in a plan. Do we use a single DAL to get data from the three, or do we deploy a DAL per machine? The idea now is to deploy a single instance of this DAL. Along with the code there is a _.ini_ file that relates machine IDs with box IP, so we can access data from any machine using a single instance of the DAL. Moreover, the DAL launches 10 workers, more than needed.

# Running isolately

* Get the environment ready.
* Download thi repo.
* Run the server with `python3 savvy-streaming-api-server.py` (Ensure the python binary points to a Python 3 version and ensure this is running in the same local network as the machines).
* Run the client with `python3 savvy-streaming-api-client.py`.
* The client calls the server with a harcoded machine ID so we should start receiving machine data.

The server listens in the port 40001.

# Running with Docker

* Run `docker build -t streaming-dal-ideko .` from the folder where the Dockerfile is.
* Run `docker run -p 40001:40001 --restart always --name streaming-dal-ideko -d 153.92.30.56:5050/streaming-dal-ideko`. The server will be automatically started.
* Run any _grpc_ client or use the included cliente by `docker exec -it ID /bin/bash` and then running the client as detailed in the previus section.

## Running the DAL in test mode

In test mode, the authorization is not parsed so any request goes on:

`docker run -p 40001:40001 --restart always --name streaming-dal-ideko 153.92.30.56:5050/streaming-dal-ideko python3 -u /opt/dal/savvy-streaming-api-server.py --testing`

# TODO

* This DAL misses IAM integration

