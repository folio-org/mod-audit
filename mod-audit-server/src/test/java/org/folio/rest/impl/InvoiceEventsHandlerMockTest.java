package org.folio.rest.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.consumer.impl.KafkaConsumerRecordImpl;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.folio.CopilotGenerated;
import org.folio.dao.acquisition.impl.InvoiceEventsDaoImpl;
import org.folio.kafka.KafkaTopicNameHelper;
import org.folio.rest.jaxrs.model.InvoiceAuditEvent;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.acquisition.InvoiceAuditEventsService;
import org.folio.services.acquisition.impl.InvoiceAuditEventsServiceImpl;
import org.folio.util.PostgresClientFactory;
import org.folio.verticle.acquisition.consumers.InvoiceEventsHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.folio.kafka.KafkaTopicNameHelper.getDefaultNameSpace;
import static org.folio.utils.EntityUtils.createInvoiceAuditEvent;
import static org.folio.utils.EntityUtils.createInvoiceAuditEventWithoutSnapshot;

@CopilotGenerated(partiallyGenerated = true)
public class InvoiceEventsHandlerMockTest {

  private static final String TENANT_ID = "diku";
  protected static final String TOKEN = "token";
  private static final String KAFKA_ENV = "folio";

  public static final String OKAPI_TOKEN_HEADER = "x-okapi-token";
  public static final String OKAPI_URL_HEADER = "x-okapi-url";

  @Spy
  private Vertx vertx = Vertx.vertx();

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());

  @Mock
  InvoiceEventsDaoImpl invoiceEventDao;
  @Mock
  InvoiceAuditEventsService invoiceAuditEventServiceImpl;

  private InvoiceEventsHandler invoiceEventsHandler;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    invoiceEventDao = new InvoiceEventsDaoImpl(postgresClientFactory);
    invoiceAuditEventServiceImpl = new InvoiceAuditEventsServiceImpl(invoiceEventDao);
    invoiceEventsHandler = new InvoiceEventsHandler(vertx, invoiceAuditEventServiceImpl);
  }

  @Test
  void shouldProcessEvent() {
    var invoiceAuditEvent = createInvoiceAuditEvent(UUID.randomUUID().toString());
    KafkaConsumerRecord<String, String> kafkaConsumerRecord = buildKafkaConsumerRecord(invoiceAuditEvent);

    Future<String> saveFuture = invoiceEventsHandler.handle(kafkaConsumerRecord);
    saveFuture.onComplete(ar -> Assertions.assertTrue(ar.succeeded()));
  }

  @Test
  void shouldNotProcessEvent() {
    var invoiceAuditEvent = createInvoiceAuditEventWithoutSnapshot();
    KafkaConsumerRecord<String, String> kafkaConsumerRecord = buildKafkaConsumerRecord(invoiceAuditEvent);

    Future<String> save = invoiceEventsHandler.handle(kafkaConsumerRecord);
    Assertions.assertTrue(save.failed());
  }

  private KafkaConsumerRecord<String, String> buildKafkaConsumerRecord(InvoiceAuditEvent kafkaConsumerRecord) {
    String topic = KafkaTopicNameHelper.formatTopicName(KAFKA_ENV, getDefaultNameSpace(), TENANT_ID, kafkaConsumerRecord.getAction().toString());
    ConsumerRecord<String, String> consumerRecord = buildConsumerRecord(topic, kafkaConsumerRecord);
    return new KafkaConsumerRecordImpl<>(consumerRecord);
  }

  protected ConsumerRecord<String, String> buildConsumerRecord(String topic, InvoiceAuditEvent event) {
    ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("folio", 0, 0, topic, Json.encode(event));
    consumerRecord.headers().add(new RecordHeader(OkapiConnectionParams.OKAPI_TENANT_HEADER, TENANT_ID.getBytes(StandardCharsets.UTF_8)));
    consumerRecord.headers().add(new RecordHeader(OKAPI_URL_HEADER, ("http://localhost:" + 8080).getBytes(StandardCharsets.UTF_8)));
    consumerRecord.headers().add(new RecordHeader(OKAPI_TOKEN_HEADER, (TOKEN).getBytes(StandardCharsets.UTF_8)));
    return consumerRecord;
  }
}
