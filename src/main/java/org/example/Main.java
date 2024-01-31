package org.example;

import com.google.cloud.opentelemetry.trace.TraceConfiguration;
import com.google.cloud.opentelemetry.trace.TraceExporter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


public class Main {

    private static final String INSTRUMENTATION_SCOPE_NAME = Main.class.getName();

    public static void main(String[] args) throws Exception {
        // Setup SDK first
        OpenTelemetrySdk openTelemetrySdk = setupTraceExporter();

        // Create spans, events and do the job
        Span spanA = openTelemetrySdk.getTracer(INSTRUMENTATION_SCOPE_NAME).spanBuilder("Span A").startSpan();
//        Span spanB = openTelemetrySdk.getTracer(INSTRUMENTATION_SCOPE_NAME).spanBuilder("Span B").startSpan();
        try (Scope scope = spanA.makeCurrent()) {
            spanA.addEvent("Event A");
            // Do some work here
            delay(2);

            spanA.addEvent("Event B");
            // Do some work here
            delay(3);
        } finally {
            spanA.end();
        }

        Span spanB = openTelemetrySdk.getTracer(INSTRUMENTATION_SCOPE_NAME).spanBuilder("Span B").startSpan();
        try (Scope scope = spanB.makeCurrent()) {
            spanB.addEvent("Event A");
            // Do some work here
            delay(3);

            spanB.addEvent("Event B");
            // Do some work here
            delay(2);
        } finally {
            spanB.end();
        }

        // End work
        CompletableResultCode completableResultCode =
                openTelemetrySdk.getSdkTracerProvider().shutdown();
        // wait till export finishes
        completableResultCode.join(10000, TimeUnit.MILLISECONDS);
    }

    private static OpenTelemetrySdk setupTraceExporter() throws IOException {
        SpanExporter traceExporter = TraceExporter.createWithConfiguration(
                TraceConfiguration.builder().setProjectId("tracepoc").build());

        // Register the TraceExporter with OpenTelemetry
        return OpenTelemetrySdk.builder()
                .setTracerProvider(
                        SdkTracerProvider.builder()
                                .addSpanProcessor(BatchSpanProcessor.builder(traceExporter).build())
                                .build())
                .buildAndRegisterGlobal();
    }

    private static void delay(int seconds) throws InterruptedException {
        Thread.sleep(seconds * 1000L);
    }
}