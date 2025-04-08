package org.folio.rest.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.consumer.impl.KafkaConsumerRecordImpl;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.folio.kafka.KafkaTopicNameHelper;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.marc.MarcAuditService;
import org.folio.util.marc.SourceRecordDomainEvent;
import org.folio.util.marc.SourceRecordType;
import org.folio.verticle.marc.consumers.MarcRecordEventsHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.folio.utils.EntityUtils.createSourceRecordDomainEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MarcRecordEventsHandlerMockTest {

  private static final String TENANT_ID = "diku";
  private static final String TOKEN = "token";
  private static final String KAFKA_ENV = "folio";
  private static final String OKAPI_TOKEN_HEADER = "x-okapi-token";
  private static final String OKAPI_URL_HEADER = "x-okapi-url";
  private static final String RECORD_TYPE_HEADER = "folio.srs.recordType";

  @Spy
  private Vertx vertx = Vertx.vertx();

  @Mock
  private MarcAuditService marcAuditService;

  private MarcRecordEventsHandler marcRecordEventsHandler;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    marcRecordEventsHandler = new MarcRecordEventsHandler(vertx, marcAuditService);
  }

  @Test
  void shouldHandleSupportedEvent() {
    var event = createSourceRecordDomainEvent();
    var kafkaConsumerRecord = buildKafkaConsumerRecord(event);

    when(marcAuditService.saveMarcDomainEvent(any())).thenReturn(Future.succeededFuture());

    var result = marcRecordEventsHandler.handle(kafkaConsumerRecord);

    result.onComplete(ar -> {
      assertTrue(ar.succeeded());
      assertEquals(event.getEventId(), ar.result());
      verify(marcAuditService, times(1)).saveMarcDomainEvent(any());
    });
  }

  @Test
  void shouldHandleDuplicateEvent() {
    var event = createSourceRecordDomainEvent();
    var kafkaConsumerRecord = buildKafkaConsumerRecord(event);

    when(marcAuditService.saveMarcDomainEvent(any())).thenReturn(Future.failedFuture(new DuplicateEventException("Duplicate event")));

    var result = marcRecordEventsHandler.handle(kafkaConsumerRecord);

    result.onComplete(ar -> {
      assertTrue(ar.succeeded());
      assertEquals(event.getEventId(), ar.result());
      verify(marcAuditService, times(1)).saveMarcDomainEvent(any());
    });
  }

  @Test
  void shouldFailOnSaveEventError() {
    var event = createSourceRecordDomainEvent();
    var kafkaConsumerRecord = buildKafkaConsumerRecord(event);

    when(marcAuditService.saveMarcDomainEvent(any())).thenReturn(Future.failedFuture(new RuntimeException("Error")));

    var result = marcRecordEventsHandler.handle(kafkaConsumerRecord);

    result.onComplete(ar -> {
      assertTrue(ar.failed());
      verify(marcAuditService, times(1)).saveMarcDomainEvent(any());
    });
  }

  @Test
  void shouldNotSaveAuditDataIfEventRecordTypeIsMarcHolding() {
    var event = createSourceRecordDomainEvent();
    event.setRecordType(SourceRecordType.MARC_HOLDING);
    var kafkaConsumerRecord = buildKafkaConsumerRecord(event);

    var result = marcRecordEventsHandler.handle(kafkaConsumerRecord);

    result.onComplete(ar -> {
      assertTrue(ar.succeeded());
      assertEquals(event.getEventId(), ar.result());
      verify(marcAuditService, never()).saveMarcDomainEvent(any());
    });
  }

  private KafkaConsumerRecord<String, String> buildKafkaConsumerRecord(SourceRecordDomainEvent event) {
    var topic = KafkaTopicNameHelper.formatTopicName(KAFKA_ENV, "marc-record", TENANT_ID, event.getEventType().name());
    var consumerRecord = buildConsumerRecord(topic, event);
    return new KafkaConsumerRecordImpl<>(consumerRecord);
  }

  private ConsumerRecord<String, String> buildConsumerRecord(String topic, SourceRecordDomainEvent event) {
    event.getEventMetadata().setEventDate(null);
    var eventJson = new JsonObject(Json.encode(event));
    // Convert LocalDateTime to String to avoid serialization issues
    if (event.getEventMetadata() != null) {
      eventJson.put("eventDate", LocalDateTime.now().toString());
    }

    var consumerRecord = new ConsumerRecord<>("folio", 0, 0, topic, eventJson.encode());
    consumerRecord.headers().add(new RecordHeader(OkapiConnectionParams.OKAPI_TENANT_HEADER, TENANT_ID.getBytes(StandardCharsets.UTF_8)));
    consumerRecord.headers().add(new RecordHeader(OKAPI_URL_HEADER, ("http://localhost:" + 8080).getBytes(StandardCharsets.UTF_8)));
    consumerRecord.headers().add(new RecordHeader(OKAPI_TOKEN_HEADER, TOKEN.getBytes(StandardCharsets.UTF_8)));
    consumerRecord.headers().add(new RecordHeader(RECORD_TYPE_HEADER, event.getRecordType().name().getBytes(StandardCharsets.UTF_8)));
    return consumerRecord;
  }
}
