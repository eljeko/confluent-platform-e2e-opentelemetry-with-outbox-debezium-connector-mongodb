
# CDC with Debezium MongoDB Source Connector and Outbox Event Router and Telemetry

# Prepare the opentelemetry and debezium interceptor libs

`cd opentelemetry-libs`

run `release-lib.sh`

This will download all the opentelemtry libs and the debezium telemetry interceptor locally. These libs will be used by docker-compose images.

# Mongo service app (Document insert)

## build the app

`cd mongo-service`

Use java 17 or higher
maven 3.8.x or higher

`mvn clean package`

java -jar target/quarkus-app/quarkus-run.jar

# Order consumer (Kafka Consumer)

## build the app

`cd order-consumer`

Use java 17 or higher
maven 3.8.x or higher

`mvn clean package`

java -jar target/quarkus-app/quarkus-run.jar

# Start the environment

Run the script:

```
scripts/bootstrap-cdc-mongo.sh
```

After the docker compose started you can run:

`01_setup_mongo.sh`

To prepare the mongo instance for both the collection and to enable the change stream to be used later by the connector.

# Create Debezium connector

Run the script:

`02_create_OTEL_connector.sh`

You can delete the connector with the script:

`92_delete_OTEL_connector.sh`

# Test the setup

On a new shell start to listen messages with the script

`10_consume_messages.sh`

And then produce some messages with script:

`20_insert_document_mongo.sh`

Now go to [Jager UI](http://localhost:16686)




**NOTE:** The Mongo Source conector used in this example will use opentelemtry for E2E tracing, specifically this version of MongoEventRouter can't customize the manes of the fields for open telemetry, in a next releases of Debezium this will be fixed by a patch.

---

# Configuration notes

Environments variables:

 ```
      OTEL_SERVICE_NAME: kafka-cp-connect
      OTEL_TRACES_EXPORTER: otlp
      OTEL_METRICS_EXPORTER: none
      OTEL_JAVAAGENT_DEBUG: false
      OTEL_EXPORTER_OTLP_PROTOCOL: "http/protobuf"
      OTEL_EXPORTER_OTLP_ENDPOINT: "http://jaeger:4318"
      OTEL_PROPAGATORS: "tracecontext"
      OTEL_TRACES_SAMPLER: always_on
      OTEL_INSTRUMENTATION_KAFKA_ENABLED: false
      ENABLE_OTEL: true
      KAFKA_OPTS: "-javaagent:/etc/kafka-connect/jars/opentelemetry-javaagent-2.18.1.jar"
```
This instruments the conenct jvm with opentelemetry:

**`KAFKA_OPTS: "-javaagent:/etc/kafka-connect/jars/opentelemetry-javaagent-2.18.1.jar"`**

# Setup for E2E tracing

[The offical documentation for debezium](https://debezium.io/documentation/reference/stable/integrations/tracing.html#_enabling_end_to_end_traceability) states:

*Enabling end-to-end traceability
Download and install the [debezium-interceptor](https://mvnrepository.com/artifact/io.debezium/debezium-interceptor/) to the Kafka Connect classpath.*

*Disable the automatic OpenTelemetry instrumentation at the Kafka producer and consumer by setting the value of `otel.instrumentation.common.default-enabled` to `false`.*

This seems to be uncorrect and you have to setup only:

**`OTEL_INSTRUMENTATION_KAFKA_ENABLED` to false** 

Disables default implementation of telemetry for kafka client (used by connect to produce and consume) this is needed to have E2E tracing otherwise a new tracing will be created.

**Connector config**

This interceptor will preserve and propagate the tracing context for E2E tracin in place of the default present in OTEL

`"producer.interceptor.classes":"io.debezium.tracing.DebeziumTracingProducerInterceptor"`
