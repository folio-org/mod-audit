package org.folio.builder.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.util.Constants.HOLDINGS_URL;
import static org.folio.util.Constants.ITEMS_URL;
import static org.folio.util.Constants.TEMPLATES_URL;
import static org.folio.util.Constants.URL_WITH_ID_PATTERN;
import static org.folio.util.Constants.USERS_URL;
import static org.folio.util.JsonPropertyFetcher.getArrayProperty;
import static org.folio.util.JsonPropertyFetcher.getObjectProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.BARCODE;
import static org.folio.util.LogEventPayloadField.FIRST_NAME;
import static org.folio.util.LogEventPayloadField.HOLDINGS_RECORD_ID;
import static org.folio.util.LogEventPayloadField.INSTANCE_ID;
import static org.folio.util.LogEventPayloadField.ITEMS;
import static org.folio.util.LogEventPayloadField.ITEM_BARCODE;
import static org.folio.util.LogEventPayloadField.ITEM_ID;
import static org.folio.util.LogEventPayloadField.LAST_NAME;
import static org.folio.util.LogEventPayloadField.NAME;
import static org.folio.util.LogEventPayloadField.PERSONAL;
import static org.folio.util.LogEventPayloadField.PERSONAL_NAME;
import static org.folio.util.LogEventPayloadField.TEMPLATE_ID;
import static org.folio.util.LogEventPayloadField.TEMPLATE_NAME;
import static org.folio.util.LogEventPayloadField.UPDATED_BY_USER_ID;
import static org.folio.util.LogEventPayloadField.USER_BARCODE;
import static org.folio.util.LogEventPayloadField.USER_ID;

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

public abstract class LogRecordBuilder {
  private static final Logger LOGGER = LoggerFactory.getLogger(LogRecordBuilder.class);

  private static final String OKAPI_URL = "x-okapi-url";
  private static final String EXCEPTION_CALLING_ENDPOINT_MSG = "Exception calling {} {}";

  private static final String PERSONAL_NAME_PATTERN = "%s, %s";

  protected final Map<String, String> okapiHeaders;
  protected final Context vertxContext;

  public LogRecordBuilder(Map<String, String> okapiHeaders, Context vertxContext) {
    this.okapiHeaders = okapiHeaders;
    this.vertxContext = vertxContext;
  }

  private CompletableFuture<JsonObject> handleGetRequest(String endpoint) {
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
          LOGGER.error(EXCEPTION_CALLING_ENDPOINT_MSG, HttpMethod.GET, endpoint);
          future.completeExceptionally(t);
          return null;
        });
    } catch (Exception e) {
      LOGGER.error(EXCEPTION_CALLING_ENDPOINT_MSG, HttpMethod.GET, endpoint);
      future.completeExceptionally(e);
    } finally {
      httpClient.closeClient();
    }
    return future;
  }

  public CompletableFuture<JsonObject> fetchUserBarcode(JsonObject payload) {
    return handleGetRequest(String.format(URL_WITH_ID_PATTERN, USERS_URL, getProperty(payload, USER_ID)))
      .thenCompose(userJson -> {
        if (nonNull(userJson)) {
          return CompletableFuture.completedFuture(payload.put(USER_BARCODE.value(), getProperty(userJson, BARCODE)));
        }
        return CompletableFuture.completedFuture(payload);
      });
  }

  public CompletableFuture<JsonObject> fetchTemplateName(JsonObject payload) {
    return handleGetRequest(String.format(URL_WITH_ID_PATTERN, TEMPLATES_URL, getProperty(extractFirstItem(payload), TEMPLATE_ID)))
      .thenCompose(templateJson -> {
        if (nonNull(templateJson)) {
          return CompletableFuture.completedFuture(payload
            .put(TEMPLATE_NAME.value(), isNull(getProperty(templateJson, NAME)) ? EMPTY : getProperty(templateJson, NAME)));
        }
        return CompletableFuture.completedFuture(payload.put(TEMPLATE_NAME.value(), EMPTY));
      });
  }

  public CompletableFuture<JsonObject> fetchPersonalName(JsonObject payload) {
    return handleGetRequest(String.format(URL_WITH_ID_PATTERN, USERS_URL, getProperty(payload, UPDATED_BY_USER_ID)))
      .thenCompose(userJson -> {
        if (nonNull(userJson)) {
          JsonObject personal = getObjectProperty(userJson, PERSONAL);
          if (nonNull(personal)) {
            return CompletableFuture.completedFuture(payload.put(PERSONAL_NAME.value(),
              String.format(PERSONAL_NAME_PATTERN, getProperty(personal, LAST_NAME), getProperty(personal, FIRST_NAME))));
          }
        }
        return CompletableFuture.completedFuture(payload);
      });
  }

  public CompletableFuture<JsonObject> fetchItemDetails(JsonObject payload) {
    return handleGetRequest(String.format(URL_WITH_ID_PATTERN, ITEMS_URL, getProperty(payload, ITEM_ID)))
      .thenCompose(itemJson -> addItemData(payload, itemJson))
      .thenCompose(json ->
        handleGetRequest(String.format(URL_WITH_ID_PATTERN, HOLDINGS_URL, getProperty(json, HOLDINGS_RECORD_ID))))
      .thenCompose(holdingJson -> addHoldingData(payload, holdingJson));
  }

  private CompletableFuture<JsonObject> addItemData(JsonObject payload, JsonObject itemJson) {
    if (nonNull(itemJson)) {
      return CompletableFuture.completedFuture(payload
        .put(ITEM_BARCODE.value(), getProperty(itemJson, BARCODE))
        .put(HOLDINGS_RECORD_ID.value(), getProperty(itemJson, HOLDINGS_RECORD_ID)));
    }
    return CompletableFuture.completedFuture(payload);
  }

  private CompletableFuture<JsonObject> addHoldingData(JsonObject payload, JsonObject holdingJson) {
    if (nonNull(holdingJson)) {
      return CompletableFuture.completedFuture(payload.put(INSTANCE_ID.value(), getProperty(holdingJson, INSTANCE_ID)));
    }
    return CompletableFuture.completedFuture(payload);
  }

  private JsonObject extractFirstItem(JsonObject payload) {
    if (getArrayProperty(payload, ITEMS).isEmpty()) {
      return new JsonObject();
    }
    return getArrayProperty(payload, ITEMS).getJsonObject(0);
  }

  private static JsonObject verifyAndExtractBody(Response response) {
    if (!Response.isSuccess(response.getCode())) {
      return null;
    }
    return response.getBody();
  }

  public abstract CompletableFuture<List<LogRecord>> buildLogRecord(JsonObject payload);
}
