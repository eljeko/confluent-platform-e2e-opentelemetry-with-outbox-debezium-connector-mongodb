/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.demo.tracing;


import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import jakarta.enterprise.context.ApplicationScoped;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.opentelemetry.context.Scope;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;

@ApplicationScoped
public class KafkaEventConsumer {

  private static final org.jboss.logging.Logger LOG = org.jboss.logging.Logger.getLogger(org.demo.tracing.KafkaEventConsumer.class);

  private static final Tracer tracer = GlobalOpenTelemetry.getTracer("mongo-order-service-consumer");

  @Incoming("mongo-order-service")
  @Acknowledgment(Acknowledgment.Strategy.MANUAL)
  public CompletionStage<Void> onMessage(KafkaRecord<String, String> message) {
    // Extract context from headers
    Context extractedContext = GlobalOpenTelemetry.getPropagators().getTextMapPropagator().extract(Context.current(), message.getHeaders(), KafkaHeaderGetter.INSTANCE);

    // Continue the trace
    return message.ack().thenRun(() -> {
      try (Scope scope = extractedContext.makeCurrent()) {

        // Add random artificial processing delay
        try {
          long sleepMillis = ThreadLocalRandom.current().nextLong(300, 500);
          Thread.sleep(sleepMillis);
          LOG.infof("Simulated processing delay: %d ms", sleepMillis);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
        }finally {
          Span span = Span.current();
          if (!span.getSpanContext().isValid()) {
            // No upstream context → start one
            span = tracer.spanBuilder("onMessage").setSpanKind(SpanKind.CONSUMER).startSpan();
          }

          LOG.infof("Kafka message with key=%s arrived", message.getKey());
          LOG.infof("Continuing trace: traceId=%s spanId=%s", span.getSpanContext().getTraceId(), span.getSpanContext().getSpanId());

          // Add custom headers
          String eventId = getHeaderAsString(message, "id");
          String eventType = getHeaderAsString(message, "event-type");
          span.setAttribute("event.id", eventId);
          span.setAttribute("event.type", eventType);

          span.addEvent("Message processed");
          span.setAttribute("processing.success", true);

          span.end();
        }


      } catch (Exception e) {
        LOG.error("Error while processing Kafka message", e);
        Span.current().recordException(e);
        Span.current().setAttribute("processing.success", false);
      }
    });
  }

  private String getHeaderAsString(KafkaRecord<?, ?> record, String name) {
    Header header = record.getHeaders().lastHeader(name);
    if (header == null) {
      throw new IllegalArgumentException("Expected record header '" + name + "' not present");
    }

    return new String(header.value(), Charset.forName("UTF-8"));
  }


  private enum KafkaHeaderGetter implements TextMapGetter<Headers> {
    INSTANCE;

    @Override
    public Iterable<String> keys(Headers headers) {
      List<String> keys = new ArrayList<>();
      for (Header h : headers) {
        keys.add(h.key());
      }
      return keys;
    }

    @Override
    public String get(Headers headers, String key) {
      Header header = headers.lastHeader(key);
      return header != null ? new String(header.value(), StandardCharsets.UTF_8) : null;
    }
  }
}
