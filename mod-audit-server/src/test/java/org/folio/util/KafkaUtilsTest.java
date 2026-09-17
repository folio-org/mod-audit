package org.folio.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.consumer.impl.KafkaConsumerRecordImpl;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.folio.CopilotGenerated;
import org.folio.rest.RestVerticle;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@UnitTest
@CopilotGenerated
class KafkaUtilsTest {

  private static final String TENANT_ID = "diku";
  private static final String TOKEN = "token";
  private static final String SYSTEM_USER_ENV_VAR = "SYSTEM_USER_ENABLED";

  @AfterEach
  void tearDown() {
    System.clearProperty(SYSTEM_USER_ENV_VAR);
  }

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

  @Test
  void shouldFormatTopicPattern() {
    var pattern = KafkaUtils.formatTopicPattern("env", "inventory.instance");
    assertEquals("(env\\.)(.*\\.)inventory.instance", pattern);
  }

  @Test
  void getConsumerRecordHeaders_positive_keepsTokenWhenSystemUserEnabledPropertyIsTrue() {
    // Arrange
    System.setProperty(SYSTEM_USER_ENV_VAR, "true");
    var consumerRecord = buildKafkaConsumerRecord();

    // Act
    var headers = KafkaUtils.getConsumerRecordHeaders(consumerRecord);

    // Assert
    assertTrue(headers.containsKey(RestVerticle.OKAPI_HEADER_TOKEN));
    assertEquals(TOKEN, headers.get(RestVerticle.OKAPI_HEADER_TOKEN));
    assertEquals(TENANT_ID, headers.get(RestVerticle.OKAPI_HEADER_TENANT));
  }

  @Test
  void getConsumerRecordHeaders_positive_removesTokenWhenSystemUserEnabledPropertyIsFalse() {
    // Arrange
    System.setProperty(SYSTEM_USER_ENV_VAR, "false");
    var consumerRecord = buildKafkaConsumerRecord();

    // Act
    var headers = KafkaUtils.getConsumerRecordHeaders(consumerRecord);

    // Assert
    assertFalse(headers.containsKey(RestVerticle.OKAPI_HEADER_TOKEN));
    assertEquals(TENANT_ID, headers.get(RestVerticle.OKAPI_HEADER_TENANT));
  }

  @Test
  void getConsumerRecordHeaders_positive_isCaseInsensitive() {
    // Arrange
    System.setProperty(SYSTEM_USER_ENV_VAR, "false");
    var consumerRecord = buildKafkaConsumerRecord();

    // Act
    var headers = KafkaUtils.getConsumerRecordHeaders(consumerRecord);

    // Assert
    assertEquals(TENANT_ID, headers.get(RestVerticle.OKAPI_HEADER_TENANT.toLowerCase()));
  }

  @Test
  void getConsumerRecordHeaders_positive_keepsTokenByDefaultWhenPropertyMissing() {
    // Arrange
    var consumerRecord = buildKafkaConsumerRecord();

    // Act
    var headers = KafkaUtils.getConsumerRecordHeaders(consumerRecord);

    // Assert
    assertTrue(headers.containsKey(RestVerticle.OKAPI_HEADER_TOKEN));
    assertEquals(TOKEN, headers.get(RestVerticle.OKAPI_HEADER_TOKEN));
    assertEquals(TENANT_ID, headers.get(RestVerticle.OKAPI_HEADER_TENANT));
  }

  private KafkaConsumerRecord<String, String> buildKafkaConsumerRecord() {
    var consumerRecord = new ConsumerRecord<>("folio.diku.users.users", 0, 0, "key", "value");
    consumerRecord.headers().add(new RecordHeader(RestVerticle.OKAPI_HEADER_TENANT, TENANT_ID.getBytes(StandardCharsets.UTF_8)));
    consumerRecord.headers().add(new RecordHeader(RestVerticle.OKAPI_HEADER_TOKEN, TOKEN.getBytes(StandardCharsets.UTF_8)));
    return new KafkaConsumerRecordImpl<>(consumerRecord);
  }
}
