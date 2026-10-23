#!/bin/bash
awslocal ssm put-parameter \
    --name "/pgsql/user" \
    --value "quarkus" \
    --type String

awslocal ssm put-parameter \
    --name "/pgsql/password" \
    --value "quarkus" \
    --type String

awslocal ssm put-parameter \
    --name "/pgsql/jdbc" \
    --value "jdbc:postgresql://localhost:5432/quarkus" \
    --type String

awslocal ssm put-parameter \
    --name "/app-db-config/db1" \
    --value '{"host": "localhost", "port": 5432 }' \
    --type String

awslocal ssm put-parameter \
    --name "/app-db-config/db2" \
    --value '{"host": "localhost", "port": 5433 }' \
    --type String