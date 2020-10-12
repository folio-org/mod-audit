package org.folio;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.folio.builder.record.CheckInRecordBuilderTest;
import org.folio.builder.record.CheckOutRecordBuilderTest;
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
  private static final int port = Integer.parseInt(System.getProperty("port", "8081"));

  private static Vertx vertx;

  @BeforeAll
  public static void globalInitialize() throws InterruptedException, ExecutionException, TimeoutException {
    vertx = Vertx.vertx();
    try {
      PostgresClient.setIsEmbedded(true);
      PostgresClient.getInstance(vertx)
        .startEmbeddedPostgres();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    JsonObject conf = new JsonObject().put("http.port", port)
      .put(HttpClientMock2.MOCK_MODE, "true");
    DeploymentOptions opt = new DeploymentOptions().setConfig(conf);
    CompletableFuture<String> deploymentComplete = new CompletableFuture<>();
    vertx.deployVerticle(RestVerticle.class.getName(), opt, res -> {
      if (res.succeeded()) {
        deploymentComplete.complete(res.result());
      } else {
        deploymentComplete.completeExceptionally(res.cause());
      }
    });
    deploymentComplete.get(60, TimeUnit.SECONDS);
    RestAssured.port = port;
    isInitialized = true;
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
  class CheckInRecordBuilderTestNested extends CheckInRecordBuilderTest {
  }

  @Nested
  class CheckOutRecordBuilderTestNested extends CheckOutRecordBuilderTest {
  }
}
