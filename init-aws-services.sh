
CRON_INTERVAL="cron(0 9 1W * ? *)" # use this cron to force event creation to every first week day of the month
AWS_REGION='us-east-1'
AWS_ACCOUNT='000000000000'
AWS_CMD="aws --endpoint-url=http://0.0.0.0:4566 --region $AWS_REGION"
EVENT_RULE_NAME="arn:aws:events:$AWS_REGION:$AWS_ACCOUNT:rule/pleo-antaeus-cron-rule"

init_services() {
  $AWS_CMD \
    events create-api-destination \
    --name "pleo-antaeus-billing-invoices-charge" \
    --http-method "POST" \
    --invocation-endpoint http://pleo-antaeus:7000/rest/v1/billing/charge \
    --invocation-rate-limit-per-second 1 \
    --connection "arn:aws:events:$AWS_REGION:$AWS_ACCOUNT:connection/any"

  $AWS_CMD \
      events create-api-destination \
      --name "pleo-antaeus-billing-invoices-overdue" \
      --http-method "POST" \
      --invocation-endpoint http://pleo-antaeus:7000/rest/v1/billing/overdue \
      --invocation-rate-limit-per-second 1 \
      --connection "arn:aws:events:$AWS_REGION:$AWS_ACCOUNT:connection/any"

  $AWS_CMD \
    events put-rule \
    --name $EVENT_RULE_NAME \
    --schedule-expression "rate(1 minute)" # cron(0 6,9,15 1W * ? *) for production use the expression

  $AWS_CMD \
    events put-rule \
    --name $EVENT_RULE_NAME \
    --schedule-expression "rate(10 minute)" # cron(0 2 2W * ? *) for production use the expression

  $AWS_CMD \
      events put-targets \
      --rule $EVENT_RULE_NAME \
      --targets "Id"="1","Arn"="arn:aws:events:$AWS_REGION:$AWS_ACCOUNT:api-destination/pleo-antaeus-billing-invoices-charge"

    $AWS_CMD \
        events put-targets \
        --rule $EVENT_RULE_NAME \
        --targets "Id"="1","Arn"="arn:aws:events:$AWS_REGION:$AWS_ACCOUNT:api-destination/pleo-antaeus-billing-invoices-overdue"

  echo 'Local AWS Services are running!'
}

init_services
