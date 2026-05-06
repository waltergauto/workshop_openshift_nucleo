#!/bin/bash

podman build -t default-route-openshift-image-registry.apps.ocpgiyu.lab.data.com.py/todo/todo-backend:v1 -f Dockerfile .
