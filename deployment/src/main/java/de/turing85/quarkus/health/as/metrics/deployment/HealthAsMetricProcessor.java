package de.turing85.quarkus.health.as.metrics.deployment;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import de.turing85.quarkus.health.as.metrics.runtime.Config;
import de.turing85.quarkus.health.as.metrics.runtime.checks.HealthChecksMetricsRegistrar;
import de.turing85.quarkus.health.as.metrics.runtime.datamapper.DefaultMappersRecorder;
import de.turing85.quarkus.health.as.metrics.runtime.datamapper.HealthResponseDataMapper;
import de.turing85.quarkus.health.as.metrics.runtime.groups.CustomHealthGroupsRecorder;
import de.turing85.quarkus.health.as.metrics.runtime.groups.HealthGroupsMetricsRegistrar;
import de.turing85.quarkus.health.as.metrics.runtime.registries.HealthRegistriesMetricsRegistrar;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.cache.deployment.spi.AdditionalCacheNameBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.smallrye.health.deployment.HealthBuildTimeConfig;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.health.api.HealthGroup;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.ParameterizedType;

class HealthAsMetricProcessor {
  private static final String FEATURE = "health-as-metrics";

  @BuildStep
  void enable(HealthBuildTimeConfig healthBuildTimeConfig,
      BuildProducer<HealthAsMetricsEnabledBuildItem> producer) {
    if (healthBuildTimeConfig.extensionsEnabled) {
      producer.produce(new HealthAsMetricsEnabledBuildItem());
    }
  }

  @BuildStep
  FeatureBuildItem feature(HealthAsMetricsEnabledBuildItem enabled) {
    return Optional.ofNullable(enabled).map(ignored -> new FeatureBuildItem(FEATURE)).orElse(null);
  }

  @BuildStep
  @Record(ExecutionTime.STATIC_INIT)
  void registerHealthGroups(HealthAsMetricsEnabledBuildItem enabled, CombinedIndexBuildItem index,
      CustomHealthGroupsRecorder customHealthGroupsRecorder,
      BuildProducer<SyntheticBeanBuildItem> syntheticBeanProducer) {
    if (Objects.nonNull(enabled)) {
      registerHealthGroupsBean(syntheticBeanProducer, customHealthGroupsRecorder,
          collectCustomGroup(index));
    }
  }

  @BuildStep
  @Record(ExecutionTime.STATIC_INIT)
  void registerMappers(HealthAsMetricsEnabledBuildItem enabled,
      BuildProducer<SyntheticBeanBuildItem> syntheticBeanProducer,
      DefaultMappersRecorder mappersRecorder) {
    if (Objects.nonNull(enabled)) {
      registerMappers(syntheticBeanProducer, mappersRecorder);
    }
  }

  @BuildStep
  void registerBeans(HealthAsMetricsEnabledBuildItem enabled,
      BuildProducer<AdditionalBeanBuildItem> beanProducer,
      BuildProducer<AdditionalCacheNameBuildItem> cacheProducer,
      BuildProducer<RunTimeConfigurationDefaultBuildItem> configProducer) {
    if (Objects.nonNull(enabled)) {
      registerAdditionalBeans(beanProducer, cacheProducer, configProducer);
    }
  }

  private static Set<String> collectCustomGroup(CombinedIndexBuildItem index) {
    // @formatter:off
    return index.getIndex()
        .getAnnotationsWithRepeatable(HealthGroup.class, index.getComputingIndex())
        .stream()
        .map(AnnotationInstance::value)
        .map(AnnotationValue::value)
        .map(String.class::cast)
        .collect(Collectors.toSet());
    // @formatter:on
  }

  private static void registerHealthGroupsBean(
      BuildProducer<SyntheticBeanBuildItem> syntheticBeanProducer,
      CustomHealthGroupsRecorder customHealthGroupsRecorder, Set<String> customHealthGroups) {
    // @formatter:off
    syntheticBeanProducer.produce(SyntheticBeanBuildItem
        .configure(Set.class)
            .addType(ParameterizedType.create(Set.class, ClassType.create(String.class)))
            .unremovable()
            .supplier(customHealthGroupsRecorder.wrap(customHealthGroups))
            .scope(Singleton.class)
            .name(HealthGroupsMetricsRegistrar.CUSTOM_HEALTH_GROUPS_BEAN_NAME)
            .addQualifier()
                .annotation(Identifier.class)
                .addValue("value", HealthGroupsMetricsRegistrar.CUSTOM_HEALTH_GROUPS_BEAN_NAME)
            .done()
        .done());
    // @formatter:on
  }

  private static void registerMappers(BuildProducer<SyntheticBeanBuildItem> syntheticBeanProducer,
      DefaultMappersRecorder mappersRecorder) {
    registerMapper(syntheticBeanProducer, mappersRecorder.booleanMapper(), "booleanMapper");
    registerMapper(syntheticBeanProducer, mappersRecorder.upDownMapper(), "upDownMapper");
    registerMapper(syntheticBeanProducer, mappersRecorder.readyNotReadyMapper(),
        "readyNotReadyMapper");
    registerMapper(syntheticBeanProducer, mappersRecorder.longMapper(), "longMapper");
    registerMapper(syntheticBeanProducer, mappersRecorder.statusMapper(), "statusMapper");
  }

  private static <T> void registerMapper(
      BuildProducer<SyntheticBeanBuildItem> syntheticBeanProducer,
      Supplier<HealthResponseDataMapper<T>> supplier, String name) {
    // @formatter:off
    syntheticBeanProducer.produce(SyntheticBeanBuildItem
        .configure(HealthResponseDataMapper.class)
            .addType(ParameterizedType.create(
                HealthResponseDataMapper.class, ClassType.create(Object.class)))
            .unremovable()
            .supplier(supplier)
            .scope(Singleton.class)
            .name(name)
            .addQualifier()
                .annotation(Identifier.class)
                .addValue("value", name)
            .done()
        .done());
    // @formatter:on
  }

  private static void registerAdditionalBeans(BuildProducer<AdditionalBeanBuildItem> beanProducer,
      BuildProducer<AdditionalCacheNameBuildItem> cacheProducer,
      BuildProducer<RunTimeConfigurationDefaultBuildItem> configProducer) {
    beanProducer.produce(AdditionalBeanBuildItem.unremovableOf(HealthGroupsMetricsRegistrar.class));
    beanProducer.produce(AdditionalBeanBuildItem.unremovableOf(HealthChecksMetricsRegistrar.class));
    beanProducer
        .produce(AdditionalBeanBuildItem.unremovableOf(HealthRegistriesMetricsRegistrar.class));

    cacheProducer.produce(new AdditionalCacheNameBuildItem(Config.CACHE_CHECK_NAME));
    cacheProducer.produce(new AdditionalCacheNameBuildItem(Config.CACHE_REGISTRY_NAME));
    configProducer.produce(new RunTimeConfigurationDefaultBuildItem(
        "quarkus.cache.caffeine.\"%s\".expire-after-write".formatted(Config.CACHE_CHECK_NAME),
        "5s"));
    configProducer.produce(new RunTimeConfigurationDefaultBuildItem(
        "quarkus.cache.caffeine.\"%s\".expire-after-write".formatted(Config.CACHE_REGISTRY_NAME),
        "5s"));
  }
}
