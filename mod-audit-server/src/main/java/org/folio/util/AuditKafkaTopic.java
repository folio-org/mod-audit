package org.folio.util;

import org.folio.kafka.services.KafkaTopic;

public enum AuditKafkaTopic implements KafkaTopic {

  LOG_RECORD("LOG_RECORD", 10);

  private final String topicName;
  private final int numPartitions;

  AuditKafkaTopic(String topicName, int numPartitions) {
    this.topicName = topicName;
    this.numPartitions = numPartitions;
  }

  @Override
  public String moduleName() {
    return "audit";
  }

  @Override
  public String topicName() {
    return topicName;
  }

  @Override
  public int numPartitions() {
    return numPartitions;
  }
}
