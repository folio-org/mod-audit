package org.folio.builder.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Context;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;
import org.folio.client.HttpClient;
import org.folio.client.OkapiHttpClient;
import org.folio.rest.jaxrs.model.LogRecord;

import io.vertx.core.json.JsonObject;
import org.folio.rest.tools.client.Response;

public abstract class LogRecordBuilderService {
  private static final String EXCEPTION_CALLING_ENDPOINT_MSG = "Exception calling {} {}";
  private static final Logger logger = LoggerFactory.getLogger(LogRecordBuilderService.class);
  protected Context context;
  protected Map<String, String> headers;
  private HttpClient httpClient;

  public LogRecordBuilderService(Context context, Map<String, String> headers) {
    this.context = context;
    this.headers = headers;
    this.httpClient = OkapiHttpClient.getInstance();
  }

  public abstract List<LogRecord> buildLogRecord(JsonObject payload);

  protected CompletableFuture<JsonObject> handleGetRequest(String endpoint) {
    CompletableFuture<JsonObject> future = new VertxCompletableFuture<>(context);
    logger.info("Calling GET {}", endpoint);
    httpClient.request(HttpMethod.GET, endpoint, headers)
      .thenApply(response -> {
        logger.debug("Validating response for GET {}", endpoint);
        return validateAndGetResponseBody(response);
      })
      .thenAccept(body -> {
        logger.debug("The response body for GET {}: {}", endpoint, body.encode());
        future.complete(body);
      })
      .handle((obj, thr) -> {
        if (thr != null) {
          logger.error(EXCEPTION_CALLING_ENDPOINT_MSG, HttpMethod.GET, endpoint);
          future.completeExceptionally(thr);
        }
        return null;
      });
    return future;
  }

  private JsonObject validateAndGetResponseBody(Response response) {
    int code = response.getCode();
    if (Response.isSuccess(code)) {
      return response.getBody();
    }
    return new JsonObject();
  }
}
