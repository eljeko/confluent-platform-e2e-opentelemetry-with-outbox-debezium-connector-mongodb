
echo "Installing mongo debezium connector..."

curl -X POST -H Accept:application/json -H Content-Type:application/json http://localhost:8083/connectors/ -d @config/debezium-source-mongo-otel.json

curl -X GET -H Accept:application/json -H Content-Type:application/json http://localhost:8083/connectors/debezium-source-mongo-otel/config|jq