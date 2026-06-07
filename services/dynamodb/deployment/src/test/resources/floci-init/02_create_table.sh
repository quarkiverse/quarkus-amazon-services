#!/usr/bin/env bash
echo "#### Create QuarkusFruits table ####"
aws dynamodb create-table \
                          --table-name QuarkusFruits \
                          --attribute-definitions AttributeName=fruitName,AttributeType=S \
                          --key-schema AttributeName=fruitName,KeyType=HASH \
                          --provisioned-throughput ReadCapacityUnits=1,WriteCapacityUnits=1 \

echo "#### Dynamodb init completed"
