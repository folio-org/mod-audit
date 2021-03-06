package org.folio;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.folio.builder.service.CheckInRecordBuilderTest;
import org.folio.builder.service.CheckOutRecordBuilderTest;
import org.folio.builder.service.FeeFineRecordBuilderTest;
import org.folio.builder.service.LoanRecordBuilderTest;
import org.folio.builder.service.LogRecordBuilderResolverTest;
import org.folio.builder.service.ManualBlockRecordBuilderTest;
import org.folio.builder.service.NoticeRecordBuilderTest;
import org.folio.builder.service.RequestRecordBuilderTest;
import org.folio.rest.RestVerticle;
import org.folio.rest.impl.AuditDataImplApiTest;
import org.folio.rest.impl.AuditHandlersImplApiTest;
import org.folio.rest.impl.CirculationLogsImplApiTest;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.client.test.HttpClientMock2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;

import io.restassured.RestAssured;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class TestSuite {
  public static boolean isInitialized = false;
  public static final int port = Integer.parseInt(System.getProperty("port", "8081"));

  private static Vertx vertx;

  @BeforeAll
  public static void globalInitialize() throws InterruptedException, ExecutionException, TimeoutException, IOException {
    Locale.setDefault(Locale.US);

    vertx = Vertx.vertx();

    PostgresClient.setIsEmbedded(true);
    PostgresClient.getInstance(vertx).startEmbeddedPostgres();

    DeploymentOptions options = new DeploymentOptions();

    options.setConfig(new JsonObject().put("http.port", port).put(HttpClientMock2.MOCK_MODE, "true"));
    options.setWorker(true);

    startVerticle(options);

    RestAssured.port = port;
    isInitialized = true;
  }

  private static void startVerticle(DeploymentOptions options)
    throws InterruptedException, ExecutionException, TimeoutException {

    CompletableFuture<String> deploymentComplete = new CompletableFuture<>();

    vertx.deployVerticle(RestVerticle.class.getName(), options, res -> {
      if(res.succeeded()) {
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

  @AfterAll
  public static void globalTearDown() {
    if (Objects.nonNull(vertx)) {
      vertx.close();
    }
    isInitialized = false;
  }

  @Nested
  class AuditDataImplApiTestNested extends AuditDataImplApiTest {
  }

  @Nested
  class CirculationLogsImplApiTestNested extends CirculationLogsImplApiTest {
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
}
