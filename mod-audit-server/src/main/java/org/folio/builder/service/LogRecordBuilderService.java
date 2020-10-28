package org.folio.builder.service;

import static java.util.Objects.nonNull;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Context;
import io.vertx.core.http.HttpMethod;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;
import org.folio.rest.jaxrs.model.LogRecord;

import io.vertx.core.json.JsonObject;
import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.Response;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.tools.utils.TenantTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LogRecordBuilderService {
  private static final Logger LOGGER = LoggerFactory.getLogger(LogRecordBuilderService.class);

  private static final String OKAPI_URL = "x-okapi-url";
  private static final String EXCEPTION_CALLING_ENDPOINT_MSG = "Exception calling {} {}";

  protected final Map<String, String> okapiHeaders;
  protected final Context vertxContext;

  public LogRecordBuilderService(Map<String, String> okapiHeaders, Context vertxContext) {
    this.okapiHeaders = okapiHeaders;
    this.vertxContext = vertxContext;
  }

  public CompletableFuture<JsonObject> handleGetRequest(String endpoint) {
    CompletableFuture<JsonObject> future = new VertxCompletableFuture<>(vertxContext);

    final String okapiURL = okapiHeaders.getOrDefault(OKAPI_URL, "");
    final String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
    HttpClientInterface httpClient = HttpClientFactory.getHttpClient(okapiURL, tenantId);

    try {
      LOGGER.info("Calling GET {}", endpoint);

      httpClient
        .request(HttpMethod.GET, endpoint, okapiHeaders)
        .thenApply(response -> {
          LOGGER.debug("Validating response for GET {}", endpoint);
          return verifyAndExtractBody(response);
        })
        .thenAccept(body -> {
          if (LOGGER.isInfoEnabled()) {
            LOGGER.info("The response body for GET {}: {}", endpoint, nonNull(body) ? body.encodePrettily() : null);
          }
          future.complete(body);
        })
        .exceptionally(t -> {
          LOGGER.error(EXCEPTION_CALLING_ENDPOINT_MSG, t, HttpMethod.GET, endpoint);
          future.completeExceptionally(t);
          return null;
        });
    } catch (Exception e) {
      LOGGER.error(EXCEPTION_CALLING_ENDPOINT_MSG, e, HttpMethod.GET, endpoint);
      future.completeExceptionally(e);
    } finally {
      httpClient.closeClient();
    }
    return future;
  }

  public static JsonObject verifyAndExtractBody(Response response) {
    if (!Response.isSuccess(response.getCode())) {
      return null;
    }
    return response.getBody();
  }

  public abstract CompletableFuture<List<LogRecord>> buildLogRecord(JsonObject payload);
}
