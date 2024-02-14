package de.turing85.quarkus.health.as.metrics.runtime.checks.datamapper;

import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class BooleanMapperRecorder {
  public Supplier<HealthResponseDataMapper<Boolean>> booleanMapper() {
    // @formatter:off
    return () -> HealthResponseDataMapper.<Boolean>builder()
        .mappableType(Boolean.class)
        .keyFilter(".*")
        .upPredicate(Boolean.TRUE::equals)
        .downPredicate(Boolean.FALSE::equals)
        .build();
    // @formatter:on
  }
}
