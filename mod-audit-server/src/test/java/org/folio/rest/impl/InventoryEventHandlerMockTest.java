package org.folio.rest.impl;

import static org.folio.kafka.KafkaTopicNameHelper.getDefaultNameSpace;
import static org.folio.utils.EntityUtils.createInventoryEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.consumer.impl.KafkaConsumerRecordImpl;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.folio.CopilotGenerated;
import org.folio.kafka.KafkaTopicNameHelper;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.inventory.InventoryEventService;
import org.folio.util.inventory.InventoryEvent;
import org.folio.util.inventory.InventoryEventType;
import org.folio.util.inventory.InventoryResourceType;
import org.folio.verticle.inventory.consumers.InventoryEventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

@CopilotGenerated
public class InventoryEventHandlerMockTest {

  private static final String TENANT_ID = "diku";
  private static final String TOKEN = "token";
  private static final String KAFKA_ENV = "folio";
  public static final String OKAPI_TOKEN_HEADER = "x-okapi-token";
  public static final String OKAPI_URL_HEADER = "x-okapi-url";

  @Spy
  private Vertx vertx = Vertx.vertx();

  @Mock
  private InventoryEventService inventoryEventService;

  private InventoryEventHandler inventoryEventHandler;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    inventoryEventHandler = new InventoryEventHandler(vertx, inventoryEventService);
  }

  @Test
  void shouldHandleSupportedEvent() {
    var event = createInventoryEvent(UUID.randomUUID().toString(), InventoryEventType.CREATE, InventoryResourceType.INSTANCE);
    var kafkaConsumerRecord = buildKafkaConsumerRecord(event);

    when(inventoryEventService.processEvent(any(), any())).thenReturn(Future.succeededFuture());

    var result = inventoryEventHandler.handle(kafkaConsumerRecord);

    result.onComplete(ar -> {
      assertTrue(ar.succeeded());
      assertEquals(event.getEventId(), ar.result());
      verify(inventoryEventService, times(1)).processEvent(any(), any());
    });
  }

  @Test
  void shouldHandleDuplicateEvent() {
    var event = createInventoryEvent(UUID.randomUUID().toString(), InventoryEventType.CREATE, InventoryResourceType.INSTANCE);
    var kafkaConsumerRecord = buildKafkaConsumerRecord(event);

    when(inventoryEventService.processEvent(any(), any())).thenReturn(Future.failedFuture(new DuplicateEventException("Duplicate event")));

    var result = inventoryEventHandler.handle(kafkaConsumerRecord);

    result.onComplete(ar -> {
      assertTrue(ar.succeeded());
      assertEquals(event.getEventId(), ar.result());
      verify(inventoryEventService, times(1)).processEvent(any(), any());
    });
  }

  @Test
  void shouldHandleUnsupportedEvent() {
    var event = createInventoryEvent(UUID.randomUUID().toString(), InventoryEventType.UNKNOWN, InventoryResourceType.INSTANCE);
    var kafkaConsumerRecord = buildKafkaConsumerRecord(event);

    var result = inventoryEventHandler.handle(kafkaConsumerRecord);

    result.onComplete(ar -> {
      assertTrue(ar.succeeded());
      assertEquals(event.getEventId(), ar.result());
      verify(inventoryEventService, never()).processEvent(any(), any());
    });
  }

  @Test
  void shouldSkipProcessingForConsortiumShadowCopyCreateEvent() {
    var event = createInventoryEvent(UUID.randomUUID().toString(), InventoryEventType.CREATE, InventoryResourceType.INSTANCE, true);
    var kafkaConsumerRecord = buildKafkaConsumerRecord(event);

    var result = inventoryEventHandler.handle(kafkaConsumerRecord);

    result.onComplete(ar -> {
      assertTrue(ar.succeeded());
      assertEquals(event.getEventId(), ar.result());
      verify(inventoryEventService, never()).processEvent(any(), any());
    });
  }

  @Test
  void shouldFailOnSaveEventError() {
    var event = createInventoryEvent(UUID.randomUUID().toString(), InventoryEventType.CREATE, InventoryResourceType.INSTANCE);
    var kafkaConsumerRecord = buildKafkaConsumerRecord(event);

    when(inventoryEventService.processEvent(any(), any())).thenReturn(Future.failedFuture(new RuntimeException("Error")));

    var result = inventoryEventHandler.handle(kafkaConsumerRecord);

    result.onComplete(ar -> {
      assertTrue(ar.failed());
      verify(inventoryEventService, times(1)).processEvent(any(), any());
    });
  }

  private KafkaConsumerRecord<String, String> buildKafkaConsumerRecord(InventoryEvent event) {
    var topic = KafkaTopicNameHelper.formatTopicName(KAFKA_ENV, getDefaultNameSpace(), TENANT_ID, event.getType().name());
    var consumerRecord = buildConsumerRecord(topic, event);
    return new KafkaConsumerRecordImpl<>(consumerRecord);
  }

  private ConsumerRecord<String, String> buildConsumerRecord(String topic, InventoryEvent event) {
    var consumerRecord = new ConsumerRecord<>("folio", 0, 0, topic, Json.encode(event));
    consumerRecord.headers().add(new RecordHeader(OkapiConnectionParams.OKAPI_TENANT_HEADER, TENANT_ID.getBytes(StandardCharsets.UTF_8)));
    consumerRecord.headers().add(new RecordHeader(OKAPI_URL_HEADER, ("http://localhost:" + 8080).getBytes(StandardCharsets.UTF_8)));
    consumerRecord.headers().add(new RecordHeader(OKAPI_TOKEN_HEADER, TOKEN.getBytes(StandardCharsets.UTF_8)));
    return consumerRecord;
  }
}