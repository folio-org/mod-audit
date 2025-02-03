package org.folio.rest.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.consumer.impl.KafkaConsumerRecordImpl;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.folio.dao.acquisition.impl.OrderLineEventsDaoImpl;
import org.folio.kafka.KafkaTopicNameHelper;
import org.folio.rest.jaxrs.model.OrderLineAuditEvent;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.acquisition.impl.OrderLineAuditEventsServiceImpl;
import org.folio.util.PostgresClientFactory;
import org.folio.verticle.acquisition.consumers.OrderLineEventsHandler;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.folio.kafka.KafkaTopicNameHelper.getDefaultNameSpace;
import static org.junit.Assert.assertTrue;

public class OrderLineEventsHandlerMockTest {

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
  OrderLineEventsDaoImpl orderLineEventsDao = new OrderLineEventsDaoImpl(postgresClientFactory);

  @InjectMocks
  OrderLineAuditEventsServiceImpl orderLineAuditEventServiceImpl = new OrderLineAuditEventsServiceImpl(orderLineEventsDao);

  @InjectMocks
  private OrderLineEventsHandler orderLineEventsHandler = new OrderLineEventsHandler(vertx, orderLineAuditEventServiceImpl);

  @Test
  void shouldProcessEvent() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("Test","TestValue");

    OrderLineAuditEvent orderLineAuditEvent = new OrderLineAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withOrderLineId(UUID.randomUUID().toString())
      .withEventDate(new Date())
      .withOrderId(UUID.randomUUID().toString())
      .withActionDate(new Date())
      .withAction(OrderLineAuditEvent.Action.CREATE)
      .withOrderLineSnapshot(jsonObject)
      .withUserId(UUID.randomUUID().toString());
    KafkaConsumerRecord<String, String> kafkaConsumerRecord = buildKafkaConsumerRecord(orderLineAuditEvent);

    Future<String> saveFuture = orderLineEventsHandler.handle(kafkaConsumerRecord);;
    saveFuture.onComplete(ar -> {
      assertTrue(ar.succeeded());
    });
  }

  private KafkaConsumerRecord<String, String> buildKafkaConsumerRecord(OrderLineAuditEvent kafkaConsumerRecord) {
    String topic = KafkaTopicNameHelper.formatTopicName(KAFKA_ENV, getDefaultNameSpace(), TENANT_ID, kafkaConsumerRecord.getAction().toString());
    ConsumerRecord<String, String> consumerRecord = buildConsumerRecord(topic, kafkaConsumerRecord);
    return new KafkaConsumerRecordImpl<>(consumerRecord);
  }

  protected ConsumerRecord<String, String> buildConsumerRecord(String topic, OrderLineAuditEvent event) {
    ConsumerRecord<String, String> consumerRecord = new ConsumerRecord("folio", 0, 0, topic, Json.encode(event));
    consumerRecord.headers().add(new RecordHeader(OkapiConnectionParams.OKAPI_TENANT_HEADER, TENANT_ID.getBytes(StandardCharsets.UTF_8)));
    consumerRecord.headers().add(new RecordHeader(OKAPI_URL_HEADER, ("http://localhost:" + 8080).getBytes(StandardCharsets.UTF_8)));
    consumerRecord.headers().add(new RecordHeader(OKAPI_TOKEN_HEADER, (TOKEN).getBytes(StandardCharsets.UTF_8)));
    return consumerRecord;
  }


}
