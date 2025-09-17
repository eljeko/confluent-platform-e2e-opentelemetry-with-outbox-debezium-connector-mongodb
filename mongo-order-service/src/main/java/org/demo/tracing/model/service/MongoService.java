package org.demo.tracing.model.service;

import com.mongodb.client.ClientSession;
import io.opentelemetry.context.Scope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.demo.tracing.model.util.TraceUtil;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@ApplicationScoped
public class MongoService {

  private static final Logger LOG = Logger.getLogger(MongoService.class);

  @Inject
  MongoClient mongoClient;

  @Inject
  Tracer tracer;

  private int percentage = 50;

  public void insertWithOutbox(String name) {
    Span span = tracer.spanBuilder("insertWithOutbox").startSpan();

    try (ClientSession session = mongoClient.startSession()) {
      session.startTransaction();

      try (Scope scope = span.makeCurrent()) {
        String traceparent = TraceUtil.getCurrentTraceParent();

        LOG.info("Current traceparent\n\t" + traceparent);

        Document propagationDoc = new Document("traceparent", traceparent);

        Document entityDoc = new Document()
            .append("name", name)
            .append("createdAt", Date.from(Instant.now()));

        String traceState = getTraceState();

        Document outboxDoc = new Document()
            .append("_id", UUID.randomUUID().toString())
            .append("aggregateType", "MyEntity")
            .append("topicName", "CONSUMER_LOAN")
            .append("aggregateId", UUID.randomUUID().toString())
            .append("eventType", "EntityCreated")
            .append("eventPayload", entityDoc)
            .append("timestamp", Date.from(Instant.now()));

        // Add tracing state with given probability
        if (ThreadLocalRandom.current().nextInt(100) < percentage) {
          outboxDoc.append(
              "tracingspancontext",
              "traceparent=" + traceparent + "\ntracestate=" + traceState);
        } else {
          outboxDoc.append(
              "tracingspancontext",
              "traceparent=" + traceparent);
        }

        getCollection("loans").insertOne(session, outboxDoc);

        session.commitTransaction();
        span.setAttribute("mongo.insert", "success");
      } catch (Exception e) {
        session.abortTransaction();
        span.recordException(e);
        throw e;
      } finally {
        span.end();
      }
    } catch (Exception ex) {
      ex.printStackTrace(); // Log this!
    }
  }

  private static String getTraceState() {
    int r = ThreadLocalRandom.current().nextInt(1, 100);
    int p = ThreadLocalRandom.current().nextInt(1, 100);
    return String.format("ot=r:%d;p:%d", r, p);
  }

  private MongoCollection<Document> getCollection(String name) {
    return mongoClient.getDatabase("outbox").getCollection(name);
  }
}
