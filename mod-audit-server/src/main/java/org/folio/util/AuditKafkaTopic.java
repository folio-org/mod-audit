package org.folio.util;

import org.folio.kafka.services.KafkaTopic;

/**
 * Enum of Kafka topics that mod-audit participates in.
 * Used by {@link org.folio.kafka.services.KafkaAdminClientService} to create topics
 * during tenant installation and by consumer/producer services to resolve full topic names.
 */
public enum AuditKafkaTopic implements KafkaTopic {

  /**
   * Circulation log-record events — the Kafka equivalent of the PubSub {@code LOG_RECORD} event type.
   * These events are produced by circulation modules (mod-circulation, etc.) and consumed by mod-audit
   * to persist circulation audit logs.
   */
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
