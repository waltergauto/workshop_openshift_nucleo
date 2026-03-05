NAME=$(echo $PWD | awk -F"/"  '{ print $NF }')
VERSION=$(podman images | grep $NAME | wc -l)

podman build -t $NAME:$((VERSION++)) -f Dockerfile .
