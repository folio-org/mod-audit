package org.folio.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import org.folio.CopilotGenerated;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
@CopilotGenerated
class KafkaUtilsTest {

  @Test
  void testGetTopicName() {
    // Arrange
    var consumerRecord = mock(KafkaConsumerRecord.class);
    when(consumerRecord.topic()).thenReturn("env.tenant.inventory.topic");

    // Act
    var topicName = KafkaUtils.getTopicName(consumerRecord);

    // Assert
    assertEquals("inventory.topic", topicName);
  }
}
