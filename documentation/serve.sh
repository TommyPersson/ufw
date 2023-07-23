#!/usr/bin/env bash

docker run --rm -it -p 8000:8000 -v ${PWD}:/docs $(docker build -q .)