package org.folio.rest.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.consumer.impl.KafkaConsumerRecordImpl;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.folio.CopilotGenerated;
import org.folio.dao.acquisition.impl.InvoiceLineEventsDaoImpl;
import org.folio.kafka.KafkaTopicNameHelper;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEvent;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.acquisition.impl.InvoiceLineAuditEventsServiceImpl;
import org.folio.util.PostgresClientFactory;
import org.folio.verticle.acquisition.consumers.InvoiceLineEventsHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.folio.kafka.KafkaTopicNameHelper.getDefaultNameSpace;

@CopilotGenerated(partiallyGenerated = true)
public class InvoiceLineEventsHandlerMockTest {

  private static final String TENANT_ID = "diku";
  protected static final String TOKEN = "token";
  private static final String KAFKA_ENV = "folio";

  public static final String OKAPI_TOKEN_HEADER = "x-okapi-token";
  public static final String OKAPI_URL_HEADER = "x-okapi-url";

  @Spy
  private Vertx vertx = Vertx.vertx();

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());

  @InjectMocks
  InvoiceLineEventsDaoImpl invoiceLineEventsDao = new InvoiceLineEventsDaoImpl(postgresClientFactory);

  @InjectMocks
  InvoiceLineAuditEventsServiceImpl invoiceLineAuditEventServiceImpl = new InvoiceLineAuditEventsServiceImpl(invoiceLineEventsDao);

  @InjectMocks
  private InvoiceLineEventsHandler invoiceLineEventsHandler = new InvoiceLineEventsHandler(vertx, invoiceLineAuditEventServiceImpl);

  @Test
  void shouldProcessEvent() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("Test", "TestValue");

    InvoiceLineAuditEvent invoiceLineAuditEvent = new InvoiceLineAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withInvoiceLineId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withInvoiceId(UUID.randomUUID().toString())
      .withActionDate(new Date())
      .withAction(InvoiceLineAuditEvent.Action.CREATE)
      .withInvoiceLineSnapshot(jsonObject)
      .withUserId(UUID.randomUUID().toString());
    KafkaConsumerRecord<String, String> kafkaConsumerRecord = buildKafkaConsumerRecord(invoiceLineAuditEvent);

    Future<String> saveFuture = invoiceLineEventsHandler.handle(kafkaConsumerRecord);
    saveFuture.onComplete(ar -> Assertions.assertTrue(ar.succeeded()));
  }

  private KafkaConsumerRecord<String, String> buildKafkaConsumerRecord(InvoiceLineAuditEvent kafkaConsumerRecord) {
    String topic = KafkaTopicNameHelper.formatTopicName(KAFKA_ENV, getDefaultNameSpace(), TENANT_ID, kafkaConsumerRecord.getAction().toString());
    ConsumerRecord<String, String> consumerRecord = buildConsumerRecord(topic, kafkaConsumerRecord);
    return new KafkaConsumerRecordImpl<>(consumerRecord);
  }

  protected ConsumerRecord<String, String> buildConsumerRecord(String topic, InvoiceLineAuditEvent event) {
    ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>(topic, 0, 0, topic, Json.encode(event));
    consumerRecord.headers().add(new RecordHeader(OkapiConnectionParams.OKAPI_TENANT_HEADER, TENANT_ID.getBytes(StandardCharsets.UTF_8)));
    consumerRecord.headers().add(new RecordHeader(OKAPI_URL_HEADER, ("http://localhost:" + 8080).getBytes(StandardCharsets.UTF_8)));
    consumerRecord.headers().add(new RecordHeader(OKAPI_TOKEN_HEADER, TOKEN.getBytes(StandardCharsets.UTF_8)));
    return consumerRecord;
  }
}
