#!/usr/bin/env bash
echo "#### Create AppConfigData config ####"

# Create application
APPLICATION_ID=$(aws appconfig create-application \
    --name my-application \
    --description "My application" \
    --query 'Id' \
    --output text)

# Create environment
ENVIRONMENT_ID=$(aws appconfig create-environment \
    --application-id "${APPLICATION_ID}" \
    --name my-application-env \
    --description "My application environment" \
    --query 'Id' \
    --output text)

# Create configuration profile
CONFIGURATION_PROFILE_ID=$(aws appconfig create-configuration-profile \
    --application-id "${APPLICATION_ID}" \
    --name my-application-config \
    --location-uri hosted \
    --type AWS.Freeform \
    --query 'Id' \
    --output text)

CONTENT=$(echo '{"application": "my-appconfigdata-application", "host": "localhost", "port": "5432"}' | base64)

# Create hosted configuration version
VERSION_NUMBER=$(aws appconfig create-hosted-configuration-version \
    --application-id "${APPLICATION_ID}" \
    --configuration-profile-id "${CONFIGURATION_PROFILE_ID}" \
    --content "${CONTENT}" \
    --content-type "application/json" \
    --query 'VersionNumber' \
    --output text \
    /dev/null)

# Create deployment strategy
DEPLOYMENT_STRATEGY_ID=$(aws appconfig create-deployment-strategy \
    --name my-application-all-at-once \
    --deployment-duration-in-minutes 0 \
    --growth-factor 100 \
    --final-bake-time-in-minutes 0 \
    --replicate-to NONE \
    --query 'Id' \
    --output text)

# Deploy the configuration version, AppConfig Data only serves deployed configurations
aws appconfig start-deployment \
    --application-id "${APPLICATION_ID}" \
    --environment-id "${ENVIRONMENT_ID}" \
    --configuration-profile-id "${CONFIGURATION_PROFILE_ID}" \
    --configuration-version "${VERSION_NUMBER}" \
    --deployment-strategy-id "${DEPLOYMENT_STRATEGY_ID}"
