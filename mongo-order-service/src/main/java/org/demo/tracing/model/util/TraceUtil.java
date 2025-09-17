package org.demo.tracing.model.util;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

public class TraceUtil {
    public static String getCurrentTraceParent() {
        SpanContext ctx = Span.current().getSpanContext();
        if (!ctx.isValid()) return null;

        return String.format("00-%s-%s-01", ctx.getTraceId(), ctx.getSpanId());
    }
}
