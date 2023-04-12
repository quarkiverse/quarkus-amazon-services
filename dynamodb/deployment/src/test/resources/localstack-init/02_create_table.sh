#!/usr/bin/env bash
echo "#### Create QuarkusFruits table ####"
aws dynamodb create-table --endpoint-url=http://localhost:4566 \
                          --table-name QuarkusFruits \
                          --attribute-definitions AttributeName=fruitName,AttributeType=S \
                          --key-schema AttributeName=fruitName,KeyType=HASH \
                          --provisioned-throughput ReadCapacityUnits=1,WriteCapacityUnits=1 \
                          --profile=localstack \
                          --region=us-east-1
echo "#### Dynamodb init completed"
