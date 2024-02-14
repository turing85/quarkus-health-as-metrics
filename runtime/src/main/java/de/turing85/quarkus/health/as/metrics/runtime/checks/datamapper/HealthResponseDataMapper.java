package de.turing85.quarkus.health.as.metrics.runtime.checks.datamapper;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.regex.Pattern;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

public interface HealthResponseDataMapper<T> {
  Class<T> mappableType();

  ToDoubleFunction<HealthCheck> upMapper(String key);

  ToDoubleFunction<HealthCheck> downMapper(String key);

  Pattern keyFilterPattern();

  boolean valueMappable(Object o);

  static <T> Impl.Builder<T> builder() {
    return new Impl.Builder<>();
  }

  class Impl<T> implements HealthResponseDataMapper<T> {
    private final Class<T> mappableType;

    private final Predicate<T> upPredicate;

    private final Predicate<T> downPredicate;

    private final Pattern keyFilterPattern;

    private final Predicate<T> valueMappablePredicate;

    // @formatter: off
    private Impl(Class<T> mappableType, Predicate<T> upPredicate, Predicate<T> downPredicate,
        String keyFilter) {
      this.mappableType = Optional.ofNullable(mappableType).orElseThrow();
      this.upPredicate = Optional.ofNullable(upPredicate).orElseThrow();
      this.downPredicate = Optional.ofNullable(downPredicate).orElseThrow();
      this.keyFilterPattern = Pattern.compile(Optional.ofNullable(keyFilter).orElseThrow());
      this.valueMappablePredicate = this.upPredicate.or(this.downPredicate);
    }
    // @formatter: on

    @Override
    public boolean valueMappable(Object o) {
      // @formatter:off
      return Optional.ofNullable(o)
          .filter(mappableType()::isInstance)
          .map(mappableType()::cast)
          .filter(valueMappablePredicate)
          .isPresent();
      // @formatter:on
    }

    @Override
    public ToDoubleFunction<HealthCheck> upMapper(String key) {
      // @formatter:off
      return check -> Optional.ofNullable(check)
          .map(HealthCheck::call)
          .flatMap(HealthCheckResponse::getData)
          .map(map -> map.get(key))
          .filter(mappableType()::isInstance)
          .map(mappableType()::cast)
          .filter(upPredicate)
          .map(unused -> 1.0)
          .orElse(0.0);
      // @formatter:on
    }

    @Override
    public ToDoubleFunction<HealthCheck> downMapper(String key) {
      // @formatter:off
      return check -> Optional.ofNullable(check)
          .map(HealthCheck::call)
          .flatMap(HealthCheckResponse::getData)
          .map(map -> map.get(key))
          .filter(mappableType()::isInstance)
          .map(mappableType()::cast)
          .filter(downPredicate)
          .map(unused -> 1.0)
          .orElse(0.0);
      // @formatter:on
    }

    public Class<T> mappableType() {
      return this.mappableType;
    }

    public Pattern keyFilterPattern() {
      return this.keyFilterPattern;
    }

    public static class Builder<T> {
      private Class<T> mappableType;
      private Predicate<T> upPredicate;
      private Predicate<T> downPredicate;
      private String keyFilter = ".*";

      public HealthResponseDataMapper<T> build() {
        // @formatter:off
        return new Impl<>(
            Objects.requireNonNull(mappableType),
            Objects.requireNonNull(upPredicate),
            Objects.requireNonNull(downPredicate),
            Objects.requireNonNull(keyFilter));
        // @formatter:on
      }

      public Builder<T> mappableType(Class<T> mappableType) {
        this.mappableType = mappableType;
        return this;
      }

      public Builder<T> upPredicate(Predicate<T> upPredicate) {
        this.upPredicate = upPredicate;
        return this;
      }

      public Builder<T> downPredicate(Predicate<T> downPredicate) {
        this.downPredicate = downPredicate;
        return this;
      }

      public Builder<T> keyFilter(String keyFilter) {
        this.keyFilter = keyFilter;
        return this;
      }
    }
  }
}
