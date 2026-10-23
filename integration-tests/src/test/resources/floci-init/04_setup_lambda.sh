#!/usr/bin/env bash
echo "#### Create lambda function ####"
workdir=$(mktemp -d)
cat > "$workdir/index.js" <<'JS'
exports.handler = async (event) => {
    let body = JSON.parse(event.body)
    const product = body.num1 * body.num2;
    const response = {
        statusCode: 200,
        body: "The product of " + body.num1 + " and " + body.num2 + " is " + product,
    };
    return response;
};
JS

# zip is not available in the floci image
python3 -m zipfile -c "$workdir/function.zip" "$workdir/index.js"
aws lambda create-function \
    --function-name lambda-hello \
    --runtime nodejs18.x \
    --zip-file "fileb://$workdir/function.zip" \
    --handler index.handler \
    --role arn:aws:iam::000000000000:role/lambda-role
