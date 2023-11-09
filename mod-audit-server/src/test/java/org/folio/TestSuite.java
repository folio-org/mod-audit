package org.folio;

import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;
import static net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig.defaultClusterConfig;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.restassured.RestAssured;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
import org.folio.builder.service.CheckInRecordBuilderTest;
import org.folio.builder.service.CheckOutRecordBuilderTest;
import org.folio.builder.service.FeeFineRecordBuilderTest;
import org.folio.builder.service.LoanRecordBuilderTest;
import org.folio.builder.service.LogRecordBuilderResolverTest;
import org.folio.builder.service.ManualBlockRecordBuilderTest;
import org.folio.builder.service.NoticeRecordBuilderTest;
import org.folio.builder.service.RequestRecordBuilderTest;
import org.folio.dao.OrderEventsDaoTest;
import org.folio.dao.OrderLineEventsDaoTest;
import org.folio.dao.PieceEventsDaoTest;
import org.folio.postgres.testing.PostgresTesterContainer;
import org.folio.rest.RestVerticle;
import org.folio.rest.impl.AuditDataAcquisitionAPITest;
import org.folio.rest.impl.AuditDataImplApiTest;
import org.folio.rest.impl.AuditHandlersImplApiTest;
import org.folio.rest.impl.CirculationLogsImplApiTest;
import org.folio.rest.impl.OrderEventsHandlerMockTest;
import org.folio.rest.impl.OrderLineEventsHandlerMockTest;
import org.folio.rest.impl.PieceEventsHandlerMockTest;
import org.folio.rest.persist.PostgresClient;
import org.folio.services.OrderAuditEventsServiceTest;
import org.folio.services.OrderLineAuditEventsServiceTest;
import org.folio.services.PieceAuditEventsServiceTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;

public class TestSuite {
  private static final String KAFKA_HOST = "KAFKA_HOST";
  private static final String KAFKA_PORT = "KAFKA_PORT";
  private static final String KAFKA_ENV = "ENV";
  private static final String KAFKA_ENV_VALUE = "test-env";

  public static boolean isInitialized = false;
  public static final int port = Integer.parseInt(System.getProperty("port", "8081"));
  public static EmbeddedKafkaCluster kafkaCluster;

  private static Vertx vertx;

  @BeforeAll
  public static void globalInitialize() throws InterruptedException, ExecutionException, TimeoutException {
    Locale.setDefault(Locale.US);

    vertx = Vertx.vertx();

    PostgresClient.setPostgresTester(new PostgresTesterContainer());

    DeploymentOptions options = new DeploymentOptions();

    options.setConfig(new JsonObject().put("http.port", port).put("mock.httpclient", "true"));
    options.setWorker(true);

    startKafkaMockServer();
    String[] hostAndPort = kafkaCluster.getBrokerList().split(":");
    System.setProperty(KAFKA_HOST, hostAndPort[0]);
    System.setProperty(KAFKA_PORT, hostAndPort[1]);
    System.setProperty(KAFKA_ENV, KAFKA_ENV_VALUE);

    startVerticle(options);

    RestAssured.port = port;
    isInitialized = true;
  }

  @AfterAll
  public static void globalTearDown() {
    closeKafkaMockServer();
    if (Objects.nonNull(vertx)) {
      vertx.close();
    }
    isInitialized = false;
  }

  private static void startKafkaMockServer() {
    kafkaCluster = provisionWith(defaultClusterConfig());
    kafkaCluster.start();
  }

  private static void closeKafkaMockServer() {
    kafkaCluster.stop();
  }

  private static void startVerticle(DeploymentOptions options)
    throws InterruptedException, ExecutionException, TimeoutException {

    CompletableFuture<String> deploymentComplete = new CompletableFuture<>();

    vertx.deployVerticle(RestVerticle.class.getName(), options, res -> {
      if (res.succeeded()) {
        deploymentComplete.complete(res.result());
      }
      else {
        deploymentComplete.completeExceptionally(res.cause());
      }
    });
    deploymentComplete.get(60, TimeUnit.SECONDS);
  }

  public static Vertx getVertx() {
    return vertx;
  }

  @Nested
  class AuditDataAcquisitionAPINestedTest extends AuditDataAcquisitionAPITest {
  }

  @Nested
  class AuditHandlersImplApiTestNested extends AuditHandlersImplApiTest {
  }

  @Nested
  class CheckInRecordBuilderNestedTest extends CheckInRecordBuilderTest {
  }

  @Nested
  class CheckOutRecordBuilderNestedTest extends CheckOutRecordBuilderTest {
  }

  @Nested
  class ManualBlockRecordBuilderNestedTest extends ManualBlockRecordBuilderTest {
  }

  @Nested
  class FeeFineRecordBuilderNestedTest extends FeeFineRecordBuilderTest {
  }

  @Nested
  class LoanRecordBuilderNestedTest extends LoanRecordBuilderTest {
  }

  @Nested
  class NoticeRecordBuilderNestedTest extends NoticeRecordBuilderTest {
  }

  @Nested
  class RequestRecordBuilderNestedTest extends RequestRecordBuilderTest {
  }

  @Nested
  class LogRecordBuilderResolverNestedTest extends LogRecordBuilderResolverTest {
  }

  @Nested
  class OrderEventsHandlerMockNestedTest extends OrderEventsHandlerMockTest {
  }

  @Nested
  class OrderAuditEventsServiceNestedTest extends OrderAuditEventsServiceTest {
  }

  @Nested
  class OrderEventsDaoNestedTest extends OrderEventsDaoTest {
  }

  @Nested
  class OrderLineEventsHandlerMockNestedTest extends OrderLineEventsHandlerMockTest {
  }

  @Nested
  class OrderLineAuditEventsServiceNestedTest extends OrderLineAuditEventsServiceTest {
  }

  @Nested
  class OrderLineEventsDaoNestedTest extends OrderLineEventsDaoTest {
  }

  @Nested
  class PieceEventsDaoNestedTest extends PieceEventsDaoTest {
  }

  @Nested
  class PieceAuditEventsServiceNestedTest extends PieceAuditEventsServiceTest {
  }

  @Nested
  class PieceEventsHandlerMockNestedTest extends PieceEventsHandlerMockTest {
  }

  @Nested
  class AuditDataImplApiTestNested extends AuditDataImplApiTest {
  }

  @Nested
  class CirculationLogsImplApiTestNested extends CirculationLogsImplApiTest {
  }

}
