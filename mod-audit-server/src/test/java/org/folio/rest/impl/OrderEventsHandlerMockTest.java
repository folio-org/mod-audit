package org.folio.rest.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.consumer.impl.KafkaConsumerRecordImpl;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.folio.dao.acquisition.impl.OrderEventsDaoImpl;
import org.folio.kafka.KafkaTopicNameHelper;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.acquisition.impl.OrderAuditEventsServiceImpl;
import org.folio.util.PostgresClientFactory;
import org.folio.verticle.acquisition.consumers.OrderEventsHandler;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.folio.kafka.KafkaTopicNameHelper.getDefaultNameSpace;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

public class OrderEventsHandlerMockTest {

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
  OrderEventsDaoImpl orderEventDao = new OrderEventsDaoImpl(postgresClientFactory);

  @InjectMocks
  OrderAuditEventsServiceImpl orderAuditEventServiceImpl = new OrderAuditEventsServiceImpl(orderEventDao);

  @InjectMocks
  private OrderEventsHandler orderEventsHandler = new OrderEventsHandler(vertx, orderAuditEventServiceImpl);

  @Test
  void shouldProcessEvent() {
    OrderAuditEvent orderAuditEvent = new OrderAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withEventDate(null)
      .withOrderId(UUID.randomUUID().toString())
      .withActionDate(null)
      .withAction(OrderAuditEvent.Action.CREATE)
      .withOrderSnapshot("{\"name\":\"New Product\"}")
      .withUserId(UUID.randomUUID().toString());
    KafkaConsumerRecord<String, String> kafkaConsumerRecord = buildKafkaConsumerRecord(orderAuditEvent);

    Future<String> saveFuture = orderEventsHandler.handle(kafkaConsumerRecord);
    saveFuture.onComplete(ar -> {
      assertTrue(ar.succeeded());
    });
  }

  private KafkaConsumerRecord<String, String> buildKafkaConsumerRecord(OrderAuditEvent record) {
    String topic = KafkaTopicNameHelper.formatTopicName(KAFKA_ENV, getDefaultNameSpace(), TENANT_ID, record.getAction().toString());
    OrderAuditEvent orderAuditEvent = new OrderAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withEventDate(null)
      .withOrderId(UUID.randomUUID().toString())
      .withActionDate(null).
      withAction(OrderAuditEvent.Action.CREATE)
      .withOrderSnapshot("")
      .withUserId(UUID.randomUUID().toString())
      .withUserName("Test");

    ConsumerRecord<String, String> consumerRecord = buildConsumerRecord(topic, orderAuditEvent);
    return new KafkaConsumerRecordImpl<>(consumerRecord);
  }

  protected ConsumerRecord<String, String> buildConsumerRecord(String topic, OrderAuditEvent event) {
    ConsumerRecord<java.lang.String, java.lang.String> consumerRecord = new ConsumerRecord("folio", 0, 0, topic, Json.encode(event));
    consumerRecord.headers().add(new RecordHeader(OkapiConnectionParams.OKAPI_TENANT_HEADER, TENANT_ID.getBytes(StandardCharsets.UTF_8)));
    consumerRecord.headers().add(new RecordHeader(OKAPI_URL_HEADER, ("http://localhost:" + 8080).getBytes(StandardCharsets.UTF_8)));
    consumerRecord.headers().add(new RecordHeader(OKAPI_TOKEN_HEADER, (TOKEN).getBytes(StandardCharsets.UTF_8)));
    return consumerRecord;
  }


}
