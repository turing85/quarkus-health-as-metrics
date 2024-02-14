package de.turing85.quarkus.health.as.metrics.runtime.groups;

import java.util.Set;
import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class CustomHealthGroupsRecorder {
  public Supplier<Set<String>> wrap(Set<String> customHealthGroups) {
    return () -> customHealthGroups;
  }
}
