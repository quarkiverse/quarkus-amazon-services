#!/usr/bin/env bash
set -euo pipefail

aws s3api create-bucket --bucket config-source-tests

echo 'value=initial' > /tmp/s3-config.properties
aws s3api put-object --bucket config-source-tests --key application.properties \
    --body /tmp/s3-config.properties --content-type application/octet-stream

echo '{"port":5432}' > /tmp/s3-config.json
aws s3api put-object --bucket config-source-tests --key 'folder/config +%?#.properties' \
    --body /tmp/s3-config.json --content-type 'application/json; charset=utf-8'

echo 'value: yaml' > /tmp/s3-config.yaml
aws s3api put-object --bucket config-source-tests --key application.yaml \
    --body /tmp/s3-config.yaml --content-type text/plain

echo 'value=explicit' > /tmp/s3-config-explicit.properties
aws s3api put-object --bucket config-source-tests --key explicit.data \
    --body /tmp/s3-config-explicit.properties --content-type application/json
