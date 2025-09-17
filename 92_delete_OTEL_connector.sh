
echo "Deleting mongo debezium connector..."

curl -X DELETE -H Accept:application/json -H Content-Type:application/json http://localhost:8083/connectors/debezium-source-mongo-otel
