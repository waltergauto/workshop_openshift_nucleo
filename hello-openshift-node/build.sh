#!/bin/bash

podman build -t default-route-openshift-image-registry.apps.ocpgiyu.lab.data.com.py/app/hello-openshift-node:v1 -f Dockerfile .
podman push default-route-openshift-image-registry.apps.ocpgiyu.lab.data.com.py/app/hello-openshift-node:v1 --tls-verify=false
