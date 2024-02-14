package de.turing85.quarkus.health.as.metrics.runtime.checks.datamapper;

import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class StringMappersRecorder {
  public Supplier<HealthResponseDataMapper<String>> upDownMapper() {
    // @formatter:off
    return () -> HealthResponseDataMapper.<String>builder()
        .mappableType(String.class)
        .keyFilter(".*")
        .upPredicate("UP"::equalsIgnoreCase)
        .downPredicate("DOWN"::equalsIgnoreCase)
        .build();
    // @formatter:on
  }

  public Supplier<HealthResponseDataMapper<String>> readyNotReadyMapper() {
    // @formatter:off
    return () -> HealthResponseDataMapper.<String>builder()
        .mappableType(String.class)
        .keyFilter(".*")
        .upPredicate("READY"::equalsIgnoreCase)
        .downPredicate("NOT READY"::equalsIgnoreCase)
        .build();
    // @formatter:on
  }
}
