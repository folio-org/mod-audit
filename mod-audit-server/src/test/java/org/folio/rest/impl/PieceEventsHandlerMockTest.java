package org.folio.rest.impl;


import static org.folio.kafka.KafkaTopicNameHelper.getDefaultNameSpace;
import static org.folio.utils.EntityUtils.createPieceAuditEvent;
import static org.folio.utils.EntityUtils.createPieceAuditEventWithoutSnapshot;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.consumer.impl.KafkaConsumerRecordImpl;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.folio.dao.acquisition.impl.PieceEventsDaoImpl;
import org.folio.kafka.KafkaTopicNameHelper;
import org.folio.rest.jaxrs.model.PieceAuditEvent;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.acquisition.PieceAuditEventsService;
import org.folio.services.acquisition.impl.PieceAuditEventsServiceImpl;
import org.folio.util.PostgresClientFactory;
import org.folio.verticle.acquisition.consumers.PieceEventsHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class PieceEventsHandlerMockTest {
  private static final String TENANT_ID = "diku";
  protected static final String TOKEN = "token";
  protected static final String KAFKA_EVN = "folio";
  public static final String OKAPI_TOKEN_HEADER = "x-okapi-token";
  public static final String OKAPI_URL_HEADER = "x-okapi-url";

  @Spy
  private Vertx vertx = Vertx.vertx();
  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());

  @Mock
  PieceEventsDaoImpl pieceEventsDao;
  @Mock
  PieceAuditEventsService pieceAuditEventsService;

  private PieceEventsHandler pieceEventsHandler;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this).close();
    pieceEventsDao = new PieceEventsDaoImpl(postgresClientFactory);
    pieceAuditEventsService = new PieceAuditEventsServiceImpl(pieceEventsDao);
    pieceEventsHandler = new PieceEventsHandler(vertx, pieceAuditEventsService);
  }

  @Test
  void shouldProcessEvent() {
    var pieceAuditEvent = createPieceAuditEvent(UUID.randomUUID().toString());
    KafkaConsumerRecord<String, String> kafkaConsumerRecord = buildKafkaConsumerRecord(pieceAuditEvent);
    Future<String> saveFuture = pieceEventsHandler.handle(kafkaConsumerRecord);
    saveFuture.onComplete(are -> {
      assertTrue(are.succeeded());
    });
  }

  @Test
  void shouldNotProcessEvent() {
    var pieceAuditEvent = createPieceAuditEventWithoutSnapshot();
    KafkaConsumerRecord<String, String> kafkaConsumerRecord = buildKafkaConsumerRecord(pieceAuditEvent);
    Future<String> saveFuture = pieceEventsHandler.handle(kafkaConsumerRecord);
    assertTrue(saveFuture.failed());
  }

  private KafkaConsumerRecord<String, String> buildKafkaConsumerRecord(PieceAuditEvent event) {
    String topic = KafkaTopicNameHelper.formatTopicName(KAFKA_EVN, getDefaultNameSpace(), TENANT_ID, event.getAction().name());
    ConsumerRecord<String, String> consumerRecord = buildConsumerRecord(topic, event);
    return new KafkaConsumerRecordImpl<>(consumerRecord);
  }

  protected ConsumerRecord<String, String> buildConsumerRecord(String topic, PieceAuditEvent event) {
    ConsumerRecord<String, String> consumer = new ConsumerRecord<>("folio", 0, 0, topic, Json.encode(event));
    consumer.headers().add(new RecordHeader(OkapiConnectionParams.OKAPI_TENANT_HEADER, TENANT_ID.getBytes(StandardCharsets.UTF_8)));
    consumer.headers().add(new RecordHeader(OKAPI_URL_HEADER, ("https://localhost:" + 8080).getBytes(StandardCharsets.UTF_8)));
    consumer.headers().add(new RecordHeader(OKAPI_TOKEN_HEADER, TOKEN.getBytes(StandardCharsets.UTF_8)));
    return consumer;
  }

}
