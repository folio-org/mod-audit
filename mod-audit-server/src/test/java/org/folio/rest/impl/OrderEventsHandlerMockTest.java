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
import org.folio.services.acquisition.OrderAuditEventsService;
import org.folio.services.acquisition.impl.OrderAuditEventsServiceImpl;
import org.folio.util.PostgresClientFactory;
import org.folio.verticle.acquisition.consumers.OrderEventsHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.folio.kafka.KafkaTopicNameHelper.getDefaultNameSpace;
import static org.folio.utils.EntityUtils.createOrderAuditEvent;
import static org.folio.utils.EntityUtils.createOrderAuditEventWithoutSnapshot;
import static org.junit.Assert.assertTrue;

public class OrderEventsHandlerMockTest {

  private static final String TENANT_ID = "diku";
  protected static final String TOKEN = "token";
  private static final String KAFKA_ENV = "folio";

  public static final String OKAPI_TOKEN_HEADER = "x-okapi-token";
  public static final String OKAPI_URL_HEADER = "x-okapi-url";

  private static final String ID = "0f2e22fc-fef3-4f88-a930-56bdca5bab46";

  @Spy
  private Vertx vertx = Vertx.vertx();

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());

  @Mock
  OrderEventsDaoImpl orderEventDao;
  @Mock
  OrderAuditEventsService orderAuditEventServiceImpl ;

  private OrderEventsHandler orderEventsHandler ;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    orderEventDao = new OrderEventsDaoImpl(postgresClientFactory);
    orderAuditEventServiceImpl = new OrderAuditEventsServiceImpl(orderEventDao);
    orderEventsHandler =new OrderEventsHandler(vertx, orderAuditEventServiceImpl);

  }

  @Test
  void shouldProcessEvent() {
    var orderAuditEvent = createOrderAuditEvent(UUID.randomUUID().toString());
    KafkaConsumerRecord<String, String> kafkaConsumerRecord = buildKafkaConsumerRecord(orderAuditEvent);

    Future<String> saveFuture = orderEventsHandler.handle(kafkaConsumerRecord);
    saveFuture.onComplete(ar -> {
      assertTrue(ar.succeeded());
    });
  }

  @Test
  void shouldNotProcessEvent() {
    var orderAuditEvent = createOrderAuditEventWithoutSnapshot();
    KafkaConsumerRecord<String, String> kafkaConsumerRecord = buildKafkaConsumerRecord(orderAuditEvent);

    Future<String> save = orderEventsHandler.handle(kafkaConsumerRecord);
    assertTrue(save.failed());
  }

  private KafkaConsumerRecord<String, String> buildKafkaConsumerRecord(OrderAuditEvent kafkaConsumerRecord) {
    String topic = KafkaTopicNameHelper.formatTopicName(KAFKA_ENV, getDefaultNameSpace(), TENANT_ID, kafkaConsumerRecord.getAction().toString());
    ConsumerRecord<String, String> consumerRecord = buildConsumerRecord(topic, kafkaConsumerRecord);
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
