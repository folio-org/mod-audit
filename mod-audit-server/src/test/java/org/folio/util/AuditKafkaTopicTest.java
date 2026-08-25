package org.folio.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class AuditKafkaTopicTest {

  @Test
  void logRecordTopicModuleNameReturnsAudit() {
    assertEquals("audit", AuditKafkaTopic.LOG_RECORD.moduleName());
  }

  @Test
  void logRecordTopicTopicNameReturnsLogRecord() {
    assertEquals("LOG_RECORD", AuditKafkaTopic.LOG_RECORD.topicName());
  }

  @Test
  void logRecordTopicModuleTopicNameReturnsDotSeparated() {
    assertEquals("audit.LOG_RECORD", AuditKafkaTopic.LOG_RECORD.moduleTopicName());
  }

  @Test
  void logRecordTopicFullTopicNameContainsEnvAndTenant() {
    String tenantId = "test-tenant";
    String fullTopicName = AuditKafkaTopic.LOG_RECORD.fullTopicName(tenantId);
    assertTrue(fullTopicName.contains(tenantId), "Full topic name should contain tenant id");
    assertTrue(fullTopicName.contains("audit"), "Full topic name should contain module name");
    assertTrue(fullTopicName.contains("LOG_RECORD"), "Full topic name should contain topic name");
  }

  @Test
  void logRecordTopicNumPartitionsReturnsTen() {
    assertEquals(10, AuditKafkaTopic.LOG_RECORD.numPartitions());
  }

  @Test
  void valuesContainsAllExpectedTopics() {
    var values = Arrays.asList(AuditKafkaTopic.values());
    assertTrue(values.contains(AuditKafkaTopic.LOG_RECORD));
    assertEquals(1, values.size());
  }
}
