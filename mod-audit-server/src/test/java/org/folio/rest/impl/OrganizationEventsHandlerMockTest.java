package org.folio.rest.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.consumer.impl.KafkaConsumerRecordImpl;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.folio.CopilotGenerated;
import org.folio.dao.acquisition.impl.OrganizationEventsDaoImpl;
import org.folio.kafka.KafkaTopicNameHelper;
import org.folio.rest.jaxrs.model.OrganizationAuditEvent;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.acquisition.OrganizationAuditEventsService;
import org.folio.services.acquisition.impl.OrganizationAuditEventsServiceImpl;
import org.folio.util.PostgresClientFactory;
import org.folio.utils.UnitTest;
import org.folio.verticle.acquisition.consumers.OrganizationEventsHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.folio.kafka.KafkaTopicNameHelper.getDefaultNameSpace;
import static org.folio.utils.EntityUtils.createOrganizationAuditEvent;
import static org.folio.utils.EntityUtils.createOrganizationAuditEventWithoutSnapshot;

@UnitTest
@ExtendWith(MockitoExtension.class)
@CopilotGenerated(partiallyGenerated = true)
public class OrganizationEventsHandlerMockTest {

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
  OrganizationEventsDaoImpl organizationEventDao;
  @Mock
  OrganizationAuditEventsService organizationAuditEventServiceImpl;

  private OrganizationEventsHandler organizationEventsHandler;

  @BeforeEach
  public void setUp() {
    organizationEventDao = new OrganizationEventsDaoImpl(postgresClientFactory);
    organizationAuditEventServiceImpl = new OrganizationAuditEventsServiceImpl(organizationEventDao);
    organizationEventsHandler = new OrganizationEventsHandler(vertx, organizationAuditEventServiceImpl);
  }

  @Test
  void shouldProcessEvent() {
    var organizationAuditEvent = createOrganizationAuditEvent(UUID.randomUUID().toString());
    KafkaConsumerRecord<String, String> kafkaConsumerRecord = buildKafkaConsumerRecord(organizationAuditEvent);

    Future<String> saveFuture = organizationEventsHandler.handle(kafkaConsumerRecord);
    saveFuture.onComplete(ar -> Assertions.assertTrue(ar.succeeded()));
  }

  @Test
  void shouldNotProcessEvent() {
    var organizationAuditEvent = createOrganizationAuditEventWithoutSnapshot();
    KafkaConsumerRecord<String, String> kafkaConsumerRecord = buildKafkaConsumerRecord(organizationAuditEvent);

    Future<String> save = organizationEventsHandler.handle(kafkaConsumerRecord);
    Assertions.assertTrue(save.failed());
  }

  private KafkaConsumerRecord<String, String> buildKafkaConsumerRecord(OrganizationAuditEvent kafkaConsumerRecord) {
    String topic = KafkaTopicNameHelper.formatTopicName(KAFKA_ENV, getDefaultNameSpace(), TENANT_ID, kafkaConsumerRecord.getAction().toString());
    ConsumerRecord<String, String> consumerRecord = buildConsumerRecord(topic, kafkaConsumerRecord);
    return new KafkaConsumerRecordImpl<>(consumerRecord);
  }

  protected ConsumerRecord<String, String> buildConsumerRecord(String topic, OrganizationAuditEvent event) {
    ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("folio", 0, 0, topic, Json.encode(event));
    consumerRecord.headers().add(new RecordHeader(OkapiConnectionParams.OKAPI_TENANT_HEADER, TENANT_ID.getBytes(StandardCharsets.UTF_8)));
    consumerRecord.headers().add(new RecordHeader(OKAPI_URL_HEADER, ("http://localhost:" + 8080).getBytes(StandardCharsets.UTF_8)));
    consumerRecord.headers().add(new RecordHeader(OKAPI_TOKEN_HEADER, (TOKEN).getBytes(StandardCharsets.UTF_8)));
    return consumerRecord;
  }
}
