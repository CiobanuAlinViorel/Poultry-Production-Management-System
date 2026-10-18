package com.ppms.shared.web;

import java.util.UUID;

public final class CorrelationId {

    private static final String CONTEXT_KEY = "traceId";

    private CorrelationId() {
    }

    public static String get() {
        return MdcHolder.get();
    }

    public static String setOrGenerate(String traceId) {
        String id = traceId != null && !traceId.isBlank() ? traceId : UUID.randomUUID().toString();
        MdcHolder.put(id);
        return id;
    }

    private static class MdcHolder {
        private static final ThreadLocal<String> TRACE_ID = ThreadLocal.withInitial(() -> null);

        static String get() {
            return TRACE_ID.get();
        }

        static void put(String value) {
            TRACE_ID.set(value);
            org.slf4j.MDC.put(CONTEXT_KEY, value);
        }
    }
}
