#!/usr/bin/env bash
echo "#### Create STS test role ####"
aws iam create-role --role-name test-role \
    --assume-role-policy-document '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"AWS":"arn:aws:iam::000000000000:root"},"Action":"sts:AssumeRole"}]}'

echo "#### Floci tests init completed"
