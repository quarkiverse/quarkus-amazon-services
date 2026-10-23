#!/usr/bin/env bash
echo "#### Create Secrets Manager secrets ####"
aws secretsmanager create-secret \
    --name pgsql/user \
    --secret-string "quarkus"

aws secretsmanager create-secret \
    --name pgsql/password \
    --secret-string "quarkus"

aws secretsmanager create-secret \
    --name pgsql/jdbc \
    --secret-string "jdbc:postgresql://localhost:5432/quarkus"

aws secretsmanager create-secret \
    --name app-db-config \
    --secret-string '{"host": "localhost", "port": 5432 }'
