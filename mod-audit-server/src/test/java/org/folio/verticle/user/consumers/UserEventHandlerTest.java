package org.folio.verticle.user.consumers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.consumer.impl.KafkaConsumerRecordImpl;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.user.UserEventService;
import org.folio.util.user.UserEvent;
import org.folio.util.user.UserEventType;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith({VertxExtension.class, MockitoExtension.class})
class UserEventHandlerTest {

  private static final String TENANT_ID = "diku";
  private static final String TOKEN = "token";

  @Spy
  private Vertx vertx = Vertx.vertx();

  @Mock
  private UserEventService userEventService;

  private UserEventHandler userEventHandler;

  @BeforeEach
  void setUp() {
    userEventHandler = new UserEventHandler(vertx, userEventService);
  }

  @Test
  void shouldHandleSupportedEvent(VertxTestContext ctx) {
    var event = createUserEvent(UserEventType.CREATED);
    var kafkaRecord = buildKafkaConsumerRecord(event);

    when(userEventService.processEvent(any(), any())).thenReturn(Future.succeededFuture());

    userEventHandler.handle(kafkaRecord)
      .onComplete(ctx.succeeding(id -> {
        assertEquals(event.getId(), id);
        verify(userEventService, times(1)).processEvent(any(), any());
        ctx.completeNow();
      }));
  }

  @Test
  void shouldHandleDuplicateEvent(VertxTestContext ctx) {
    var event = createUserEvent(UserEventType.CREATED);
    var kafkaRecord = buildKafkaConsumerRecord(event);

    when(userEventService.processEvent(any(), any()))
      .thenReturn(Future.failedFuture(new DuplicateEventException("Duplicate event")));

    userEventHandler.handle(kafkaRecord)
      .onComplete(ctx.succeeding(id -> {
        assertEquals(event.getId(), id);
        verify(userEventService, times(1)).processEvent(any(), any());
        ctx.completeNow();
      }));
  }

  @Test
  void shouldHandleUnsupportedEvent(VertxTestContext ctx) {
    var event = createUserEvent(UserEventType.UNKNOWN);
    var kafkaRecord = buildKafkaConsumerRecord(event);

    userEventHandler.handle(kafkaRecord)
      .onComplete(ctx.succeeding(id -> {
        assertEquals(event.getId(), id);
        verify(userEventService, never()).processEvent(any(), any());
        ctx.completeNow();
      }));
  }

  @Test
  void shouldFailOnProcessEventError(VertxTestContext ctx) {
    var event = createUserEvent(UserEventType.UPDATED);
    var kafkaRecord = buildKafkaConsumerRecord(event);

    when(userEventService.processEvent(any(), any()))
      .thenReturn(Future.failedFuture(new RuntimeException("Error")));

    userEventHandler.handle(kafkaRecord)
      .onComplete(ctx.failing(cause -> {
        verify(userEventService, times(1)).processEvent(any(), any());
        ctx.completeNow();
      }));
  }

  private UserEvent createUserEvent(UserEventType type) {
    return UserEvent.builder()
      .id(UUID.randomUUID().toString())
      .type(type)
      .tenant(TENANT_ID)
      .timestamp(System.currentTimeMillis())
      .newValue(Map.of("key", "value"))
      .build();
  }

  private KafkaConsumerRecord<String, String> buildKafkaConsumerRecord(UserEvent event) {
    var userId = UUID.randomUUID().toString();
    var consumerRecord = new ConsumerRecord<>("folio.diku.users.users", 0, 0, userId, Json.encode(event));
    consumerRecord.headers().add(new RecordHeader(OkapiConnectionParams.OKAPI_TENANT_HEADER, TENANT_ID.getBytes(StandardCharsets.UTF_8)));
    consumerRecord.headers().add(new RecordHeader("x-okapi-url", "http://localhost:8080".getBytes(StandardCharsets.UTF_8)));
    consumerRecord.headers().add(new RecordHeader("x-okapi-token", TOKEN.getBytes(StandardCharsets.UTF_8)));
    return new KafkaConsumerRecordImpl<>(consumerRecord);
  }
}
