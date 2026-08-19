package org.folio.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.utils.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class AuditKafkaTopicTest {

  @Test
  void logRecordTopicModuleNameReturnsAudit() {
    assertThat(AuditKafkaTopic.LOG_RECORD.moduleName()).isEqualTo("audit");
  }

  @Test
  void logRecordTopicTopicNameReturnsLogRecord() {
    assertThat(AuditKafkaTopic.LOG_RECORD.topicName()).isEqualTo("LOG_RECORD");
  }

  @Test
  void logRecordTopicModuleTopicNameReturnsDotSeparated() {
    assertThat(AuditKafkaTopic.LOG_RECORD.moduleTopicName()).isEqualTo("audit.LOG_RECORD");
  }

  @Test
  void logRecordTopicFullTopicNameContainsEnvAndTenant() {
    String tenantId = "test-tenant";
    String fullName = AuditKafkaTopic.LOG_RECORD.fullTopicName(tenantId);

    assertThat(fullName)
      .contains(tenantId)
      .contains("audit")
      .contains("LOG_RECORD");
  }

  @Test
  void logRecordTopicNumPartitionsReturnsTen() {
    assertThat(AuditKafkaTopic.LOG_RECORD.numPartitions()).isEqualTo(10);
  }

  @Test
  void valuesContainsAllExpectedTopics() {
    assertThat(AuditKafkaTopic.values()).hasSize(1);
    assertThat(AuditKafkaTopic.values()).contains(AuditKafkaTopic.LOG_RECORD);
  }
}
