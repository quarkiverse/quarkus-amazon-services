#!/bin/bash
awslocal secretsmanager create-secret \
    --name pgsql/user \
    --secret-string "quarkus"

awslocal secretsmanager create-secret \
    --name pgsql/password \
    --secret-string "quarkus"

awslocal secretsmanager create-secret \
    --name pgsql/jdbc \
    --secret-string "jdbc:postgresql://localhost:5432/quarkus"
    