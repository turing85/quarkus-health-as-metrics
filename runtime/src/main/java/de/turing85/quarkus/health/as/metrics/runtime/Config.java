package de.turing85.quarkus.health.as.metrics.runtime;

public class Config {
  public static final String CACHE_CHECK_NAME = "health-check-data";
  public static final String CACHE_REGISTRY_NAME = "health-registry-data";
  public static final String INDIVIDUAL_CHECK_NAME = "application.health-check";
  public static final String TAG_CHECK = "check";
  public static final String TAG_STATUS = "status";

  private Config() {
    throw new UnsupportedOperationException("this class cannot be instantiated");
  }
}
