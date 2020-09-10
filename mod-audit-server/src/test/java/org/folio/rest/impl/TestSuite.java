package org.folio.rest.impl;

import io.restassured.RestAssured;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.folio.rest.RestVerticle;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.client.test.HttpClientMock2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class TestSuite {
  public static boolean isInitialized = false;
  private static final int port = Integer.parseInt(System.getProperty("port", "8081"));

  private static Vertx vertx;

  @BeforeAll
  public static void globalInitialize() throws InterruptedException, ExecutionException, TimeoutException {
    vertx = Vertx.vertx();
    try {
      PostgresClient.setIsEmbedded(true);
      PostgresClient.getInstance(vertx).startEmbeddedPostgres();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    JsonObject conf = new JsonObject().put("http.port", port).put(HttpClientMock2.MOCK_MODE, "true");
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
  class AuditDataImplTestNested extends AuditDataImplTest {
  }

  @Nested
  class CirculationLogsImplTestNested extends CirculationLogsImplTest {
  }

  @Nested
  class AuditHandlersImplTestNested extends AuditHandlersImplTest {
  }
}
