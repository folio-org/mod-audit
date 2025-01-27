package org.folio.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import org.folio.CopilotGenerated;
import org.junit.jupiter.api.Test;

@CopilotGenerated
class KafkaUtilsTest {

  @Test
  void testGetTopicName() {
    // Arrange
    var consumerRecord = mock(KafkaConsumerRecord.class);
    when(consumerRecord.topic()).thenReturn("prefix1.prefix2.topicName");

    // Act
    var topicName = KafkaUtils.getTopicName(consumerRecord);

    // Assert
    assertEquals("topicName", topicName);
  }
}