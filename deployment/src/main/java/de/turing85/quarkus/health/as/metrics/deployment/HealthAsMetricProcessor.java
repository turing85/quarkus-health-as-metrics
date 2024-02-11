package de.turing85.quarkus.health.as.metrics.deployment;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import de.turing85.quarkus.health.as.metrics.runtime.CustomHealthGroupsRecorder;
import de.turing85.quarkus.health.as.metrics.runtime.HealthMetricsRegistrar;
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
      BuildProducer<AdditionalBeanBuildItem> beanProducer,
      BuildProducer<SyntheticBeanBuildItem> syntheticBeanProducer,
      CustomHealthGroupsRecorder customHealthGroupsRecorder) {
    if (healthBuildTimeConfig.extensionsEnabled) {
      registerCustomGroupsBean(syntheticBeanProducer, customHealthGroupsRecorder,
          collectCustomGroup(index));
      beanProducer.produce(AdditionalBeanBuildItem.unremovableOf(HealthMetricsRegistrar.class));
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
            .name(HealthMetricsRegistrar.CUSTOM_HEALTH_GROUPS_BEAN_NAME)
            .addQualifier()
                .annotation(Identifier.class)
                .addValue("value", HealthMetricsRegistrar.CUSTOM_HEALTH_GROUPS_BEAN_NAME)
            .done()
        .done());
    // @formatter:on
  }
}
