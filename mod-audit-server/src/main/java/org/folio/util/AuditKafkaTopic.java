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
  LOG_RECORD("circulation", "log_record");

  private final String moduleName;
  private final String topicName;

  AuditKafkaTopic(String moduleName, String topicName) {
    this.moduleName = moduleName;
    this.topicName = topicName;
  }

  @Override
  public String moduleName() {
    return moduleName;
  }

  @Override
  public String topicName() {
    return topicName;
  }
}
