package org.folio.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.utils.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class AuditKafkaTopicTest {

  @Test
  void logRecordTopic_moduleName_returnsCirculation() {
    assertThat(AuditKafkaTopic.LOG_RECORD.moduleName()).isEqualTo("circulation");
  }

  @Test
  void logRecordTopic_topicName_returnsLogRecord() {
    assertThat(AuditKafkaTopic.LOG_RECORD.topicName()).isEqualTo("log_record");
  }

  @Test
  void logRecordTopic_moduleTopicName_returnsDotSeparated() {
    assertThat(AuditKafkaTopic.LOG_RECORD.moduleTopicName()).isEqualTo("circulation.log_record");
  }

  @Test
  void logRecordTopic_fullTopicName_containsEnvAndTenant() {
    String tenantId = "test-tenant";
    String fullName = AuditKafkaTopic.LOG_RECORD.fullTopicName(tenantId);

    assertThat(fullName).contains(tenantId);
    assertThat(fullName).contains("circulation");
    assertThat(fullName).contains("log_record");
  }

  @Test
  void values_containsAllExpectedTopics() {
    assertThat(AuditKafkaTopic.values()).hasSize(1);
    assertThat(AuditKafkaTopic.values()).contains(AuditKafkaTopic.LOG_RECORD);
  }
}
