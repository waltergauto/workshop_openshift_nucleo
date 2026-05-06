#!/bin/bash

podman build -t default-route-openshift-image-registry.apps.ocpgiyu.lab.data.com.py/app/simple-spring:v1 -f Dockerfile .
podman push default-route-openshift-image-registry.apps.ocpgiyu.lab.data.com.py/app/simple-spring:v1 --tls-verify=false
