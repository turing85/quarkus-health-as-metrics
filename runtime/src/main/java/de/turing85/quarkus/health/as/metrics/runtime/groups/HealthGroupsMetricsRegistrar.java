package de.turing85.quarkus.health.as.metrics.runtime.groups;

import java.util.Objects;
import java.util.Set;
import java.util.function.ToDoubleFunction;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;

@Singleton
public class HealthGroupsMetricsRegistrar {
  public static final String CUSTOM_HEALTH_GROUPS_BEAN_NAME = "customHealthGroups";

  private static final String STATUS_CHECK_NAME = "application.status";
  private static final String TAG_GROUP = "group";
  private static final String TAG_STATUS = "status";

  private final MeterRegistry registry;
  private final SmallRyeHealthReporter healthReporter;
  private final Set<String> customGroups;

  public HealthGroupsMetricsRegistrar(MeterRegistry registry, SmallRyeHealthReporter healthReporter,

      @Identifier(CUSTOM_HEALTH_GROUPS_BEAN_NAME)
      @SuppressWarnings("CdiInjectionPointsInspection") Set<String> customGroups) {
    this.registry = registry;
    this.healthReporter = healthReporter;
    this.customGroups = customGroups;
  }// @formatter:off

  void register(@Observes StartupEvent ignored) {
    registerHealthMetricsToRegistry();
    registerLivenessMetricsToRegistry();
    registerReadinessMetricsToRegistry();
    registerStartupMetricsToRegistry();
    registerWellnessMetricsToRegistry();

    customGroups.forEach(this::registerCustomGroup);
  }

  private void registerHealthMetricsToRegistry() {
    // @formatter:off
    registerReporters(
        "health",
        r -> statusToIntForUp(r.getHealth()),
        r -> statusToIntForDown(r.getHealth()));
    // @formatter:on
  }

  private void registerLivenessMetricsToRegistry() {
    // @formatter:off
    registerReporters(
        "live",
        r -> statusToIntForUp(r.getLiveness()),
        r -> statusToIntForDown(r.getLiveness()));
    // @formatter:on
  }

  private void registerReadinessMetricsToRegistry() {
    // @formatter:off
    registerReporters(
        "ready",
        r -> statusToIntForUp(r.getReadiness()),
        r -> statusToIntForDown(r.getReadiness()));
    // @formatter:on
  }

  private void registerStartupMetricsToRegistry() {
    // @formatter:off
    registerReporters(
        "startup",
        r -> statusToIntForUp(r.getStartup()),
        r -> statusToIntForDown(r.getStartup()));
    // @formatter:on
  }

  private void registerWellnessMetricsToRegistry() {
    // @formatter:off
    registerReporters(
        "well",
        r -> statusToIntForUp(r.getWellness()),
        r -> statusToIntForDown(r.getWellness()));
    // @formatter:on
  }

  private void registerCustomGroup(String groupName) {
    if (Objects.nonNull(healthReporter.getHealthGroup(groupName))) {
      // @formatter:off
      registerReporters(
          groupName,
          r -> statusToIntForUp(r.getHealthGroup(groupName)),
          r -> statusToIntForDown(r.getHealthGroup(groupName)));
      // @formatter:on
    }
  }

  private static int statusToIntForUp(SmallRyeHealth health) {
    return health.isDown() ? 0 : 1;
  }

  private static int statusToIntForDown(SmallRyeHealth health) {
    return health.isDown() ? 1 : 0;
  }

  private void registerReporters(String name, ToDoubleFunction<SmallRyeHealthReporter> upMapper,
      ToDoubleFunction<SmallRyeHealthReporter> downMapper) {
    registerReporter(name, "UP", upMapper);
    registerReporter(name, "DOWN", downMapper);
  }

  private void registerReporter(String name, String statusTag,
      ToDoubleFunction<SmallRyeHealthReporter> mapper) {
    // @formatter:off
    Gauge.builder(STATUS_CHECK_NAME, healthReporter, mapper)
        .tag(TAG_GROUP, name)
        .tag(TAG_STATUS, statusTag)
        .strongReference(true)
        .register(registry);
    // @formatter:on
  }
}
