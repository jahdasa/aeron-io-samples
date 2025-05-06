## 1. Download K6
https://k6.io/docs/get-started/installation/#windows \
https://objects.githubusercontent.com/github-production-release-asset-2e65be/54400687/0c970ba6-5dac-4ee7-ba30-4640cd9fc9c9?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAIWNJYAX4CSVEH53A%2F20231129%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20231129T112726Z&X-Amz-Expires=300&X-Amz-Signature=0d6f6038231ef2bcfef677ed418c134a19cc9d31894109e13ecea70871dc6312&X-Amz-SignedHeaders=host&actor_id=0&key_id=0&repo_id=54400687&response-content-disposition=attachment%3B%20filename%3Dk6-v0.47.0-windows-amd64.msi&response-content-type=application%2Foctet-stream

## 2. Update Request
set authorization Header\
change x-dark state

## 2. Run K6
$project_dir\.k6-scripts>k6 run --vus 1 --duration 60s 001.placeorder.js

k6 run --vus 1 --duration 60s 002.online-calculate.js