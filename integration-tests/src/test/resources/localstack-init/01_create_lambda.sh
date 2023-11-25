#!/bin/bash
echo "#### Create lambda function ####"
# https://docs.localstack.cloud/user-guide/aws/lambda/#create-a-lambda-function
cat > index.js <<'EOF'
exports.handler = async (event) => {
    let body = JSON.parse(event.body)
    const product = body.num1 * body.num2;
    const response = {
        statusCode: 200,
        body: "The product of " + body.num1 + " and " + body.num2 + " is " + product,
    };
    return response;
};
EOF

zip function.zip index.js
awslocal lambda create-function \
    --function-name localstack-lambda-hello \
    --runtime nodejs18.x \
    --zip-file fileb://function.zip \
    --handler index.handler \
    --role arn:aws:iam::000000000000:role/lambda-role
