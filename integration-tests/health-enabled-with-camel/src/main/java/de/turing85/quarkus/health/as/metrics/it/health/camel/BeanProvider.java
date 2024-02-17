package de.turing85.quarkus.health.as.metrics.it.health.camel;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import de.turing85.quarkus.health.as.metrics.runtime.datamappers.HealthResponseDataMapper;
import io.smallrye.common.annotation.Identifier;

@Dependent
public class BeanProvider {
  @Produces
  @Singleton
  @Identifier("fooBarMapper")
  HealthResponseDataMapper<String> fooBarMapper() {
    // @formatter:off
    return HealthResponseDataMapper.<String>builder()
        .mappableType(String.class)
        .keyFilter(".*")
        .upPredicate("FOO"::equalsIgnoreCase)
        .downPredicate("BAR"::equalsIgnoreCase)
        .build();
    // @formatter:on
  }
}
