#!/usr/bin/env bash
echo "#### Create Scheduler target queue ####"
aws sqs create-queue --queue-name quarkus-scheduler-test-queue
