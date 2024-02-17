package de.turing85.quarkus.health.as.metrics.runtime.datamappers;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.regex.Pattern;

import io.smallrye.health.registry.HealthRegistryImpl;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

public interface HealthResponseDataMapper<T> {
  Class<T> mappableType();

  ToDoubleFunction<HealthCheck> checkUpMapper(String key);

  ToDoubleFunction<HealthRegistryImpl> registryUpMapper();

  ToDoubleFunction<HealthCheck> checkDownMapper(String key);

  ToDoubleFunction<HealthRegistryImpl> registryDownMapper();

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
    public ToDoubleFunction<HealthCheck> checkUpMapper(String key) {
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
    public ToDoubleFunction<HealthRegistryImpl> registryUpMapper() {
      // @formatter:off
      return registry -> this.getResponseFromRegistry(registry)
          .filter(upPredicate)
          .map(unused -> 1.0)
          .orElse(0.0);
      // @formatter:on
    }

    private Optional<T> getResponseFromRegistry(HealthRegistryImpl healthRegistry) {
      // @formatter:off
      return Optional.ofNullable(healthRegistry)
          .map(registry -> registry.getChecks(Map.of()))
          .stream().flatMap(Collection::stream)
          .map(Uni::subscribeAsCompletionStage)
          .map(Impl::pureGetCompletableFuture)
          .filter(Objects::nonNull)
          .map(HealthCheckResponse::getStatus)
          .filter(this::valueMappable)
          .map(this.mappableType::cast)
          .findFirst();
      // @formatter:on
    }

    private static <T> T pureGetCompletableFuture(CompletableFuture<T> future) {
      try {
        return future.get();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return null;
      } catch (ExecutionException e) {
        return null;
      }
    }

    @Override
    public ToDoubleFunction<HealthCheck> checkDownMapper(String key) {
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

    @Override
    public ToDoubleFunction<HealthRegistryImpl> registryDownMapper() {
      // @formatter:off
      return registry -> this.getResponseFromRegistry(registry)
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
