package de.turing85.quarkus.health.as.metrics.runtime.checks.datamapper;

import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;

import static java.util.function.Predicate.not;

@Recorder
public class DefaultMappersRecorder {
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

  public Supplier<HealthResponseDataMapper<Long>> longMapper() {
    // @formatter:off
    return () -> HealthResponseDataMapper.<Long>builder()
        .mappableType(Long.class)
        .keyFilter(".*")
        .upPredicate(not(Long.valueOf(0)::equals))
        .downPredicate(Long.valueOf(0)::equals)
        .build();
    // @formatter:on
  }
}
