package org.folio.verticle.logrecord.consumers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.consumer.impl.KafkaConsumerRecordImpl;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.rest.RestVerticle;
import org.folio.services.logrecord.LogRecordService;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith({VertxExtension.class, MockitoExtension.class})
class LogRecordEventHandlerTest {

  private static final String TENANT_ID = "diku";
  private static final String TOKEN = "token";
  private static final String LOG_EVENT_TYPE = "CHECK_IN_EVENT";

  @Mock
  private LogRecordService logRecordService;

  private LogRecordEventHandler logRecordEventHandler;

  @BeforeEach
  void setUp(Vertx vertx) {
    logRecordEventHandler = new LogRecordEventHandler(vertx, logRecordService);
  }

  @Test
  void shouldHandleSupportedEvent(VertxTestContext ctx) {
    var kafkaRecord = buildKafkaConsumerRecord(LOG_EVENT_TYPE);

    when(logRecordService.processLogRecord(anyString(), any(JsonObject.class), anyMap(), any()))
      .thenReturn(Future.succeededFuture());

    logRecordEventHandler.handle(kafkaRecord)
      .onComplete(ctx.succeeding(key -> {
        assertEquals(kafkaRecord.key(), key);
        verify(logRecordService, times(1))
          .processLogRecord(eq(LOG_EVENT_TYPE), any(JsonObject.class), anyMap(), any());
        ctx.completeNow();
      }));
  }

  @Test
  void shouldHandleDuplicateEvent(VertxTestContext ctx) {
    var kafkaRecord = buildKafkaConsumerRecord(LOG_EVENT_TYPE);

    when(logRecordService.processLogRecord(anyString(), any(JsonObject.class), anyMap(), any()))
      .thenReturn(Future.failedFuture(new DuplicateEventException("Duplicate event")));

    logRecordEventHandler.handle(kafkaRecord)
      .onComplete(ctx.succeeding(key -> {
        assertEquals(kafkaRecord.key(), key);
        verify(logRecordService, times(1))
          .processLogRecord(eq(LOG_EVENT_TYPE), any(JsonObject.class), anyMap(), any());
        ctx.completeNow();
      }));
  }

  @Test
  void shouldFailOnProcessLogRecordError(VertxTestContext ctx) {
    var kafkaRecord = buildKafkaConsumerRecord(LOG_EVENT_TYPE);

    when(logRecordService.processLogRecord(anyString(), any(JsonObject.class), anyMap(), any()))
      .thenReturn(Future.failedFuture(new RuntimeException("Error")));

    logRecordEventHandler.handle(kafkaRecord)
      .onComplete(ctx.failing(cause -> {
        verify(logRecordService, times(1))
          .processLogRecord(eq(LOG_EVENT_TYPE), any(JsonObject.class), anyMap(), any());
        ctx.completeNow();
      }));
  }

  @Test
  void shouldAckAndSkipProcessingForMalformedPayload(VertxTestContext ctx) {
    var recordKey = UUID.randomUUID().toString();
    var consumerRecord = new ConsumerRecord<>("folio.diku.audit.LOG_RECORD", 0, 0, recordKey, "not-a-json-payload");
    addOkapiHeaders(consumerRecord);
    var kafkaRecord = new KafkaConsumerRecordImpl<String, String>(consumerRecord);

    logRecordEventHandler.handle(kafkaRecord)
      .onComplete(ctx.succeeding(key -> {
        assertEquals(recordKey, key);
        verify(logRecordService, never()).processLogRecord(any(), any(), any(), any());
        ctx.completeNow();
      }));
  }

  private KafkaConsumerRecord<String, String> buildKafkaConsumerRecord(String logEventType) {
    var recordKey = UUID.randomUUID().toString();
    var payload = new JsonObject().put("logEventType", logEventType);
    var consumerRecord = new ConsumerRecord<>("folio.diku.audit.LOG_RECORD", 0, 0, recordKey, payload.encode());
    addOkapiHeaders(consumerRecord);
    return new KafkaConsumerRecordImpl<>(consumerRecord);
  }

  private void addOkapiHeaders(ConsumerRecord<String, String> consumerRecord) {
    consumerRecord.headers().add(new RecordHeader(RestVerticle.OKAPI_HEADER_TENANT, TENANT_ID.getBytes(StandardCharsets.UTF_8)));
    consumerRecord.headers().add(new RecordHeader("x-okapi-url", "http://localhost:8080".getBytes(StandardCharsets.UTF_8)));
    consumerRecord.headers().add(new RecordHeader("x-okapi-token", TOKEN.getBytes(StandardCharsets.UTF_8)));
  }
}
