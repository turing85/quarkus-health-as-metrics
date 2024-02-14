package de.turing85.quarkus.health.as.metrics.deployment;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import de.turing85.quarkus.health.as.metrics.runtime.checks.HealthChecksMetricsRegistrar;
import de.turing85.quarkus.health.as.metrics.runtime.checks.datamapper.BooleanMapperRecorder;
import de.turing85.quarkus.health.as.metrics.runtime.checks.datamapper.HealthResponseDataMapper;
import de.turing85.quarkus.health.as.metrics.runtime.checks.datamapper.StringMappersRecorder;
import de.turing85.quarkus.health.as.metrics.runtime.groups.CustomHealthGroupsRecorder;
import de.turing85.quarkus.health.as.metrics.runtime.groups.HealthGroupsMetricsRegistrar;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.smallrye.health.deployment.HealthBuildTimeConfig;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.health.api.HealthGroup;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.ParameterizedType;

class HealthAsMetricProcessor {
  private static final String FEATURE = "quarkus-health-as-metrics";

  @BuildStep
  FeatureBuildItem feature() {
    return new FeatureBuildItem(FEATURE);
  }

  @BuildStep
  @Record(ExecutionTime.STATIC_INIT)
  void enable(HealthBuildTimeConfig healthBuildTimeConfig, CombinedIndexBuildItem index,
      BuildProducer<SyntheticBeanBuildItem> syntheticBeanProducer,
      CustomHealthGroupsRecorder customHealthGroupsRecorder,
      StringMappersRecorder stringMappersRecorder, BooleanMapperRecorder booleanMapperRecorder,
      BuildProducer<AdditionalBeanBuildItem> beanProducer) {
    if (healthBuildTimeConfig.extensionsEnabled) {
      registerCustomGroupsBean(syntheticBeanProducer, customHealthGroupsRecorder,
          collectCustomGroup(index));
      registerMappers(syntheticBeanProducer, stringMappersRecorder, booleanMapperRecorder);

      beanProducer
          .produce(AdditionalBeanBuildItem.unremovableOf(HealthGroupsMetricsRegistrar.class));
      beanProducer
          .produce(AdditionalBeanBuildItem.unremovableOf(HealthChecksMetricsRegistrar.class));
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

  private static void registerCustomGroupsBean(
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
      StringMappersRecorder stringMappersRecorder, BooleanMapperRecorder booleanMapperRecorder) {
    registerMapper(syntheticBeanProducer, stringMappersRecorder.upDownMapper(), "UpDownMapper");
    registerMapper(syntheticBeanProducer, stringMappersRecorder.readyNotReadyMapper(),
        "ReadyNotReadyMapper");
    registerMapper(syntheticBeanProducer, booleanMapperRecorder.booleanMapper(), "BooleanMapper");
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
}
