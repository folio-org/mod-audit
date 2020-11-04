package org.folio.builder.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
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
import static org.folio.util.LogEventPayloadField.USER_BARCODE;
import static org.folio.util.LogEventPayloadField.USER_ID;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.Response;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.tools.utils.TenantTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Context;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;
import one.util.streamex.StreamEx;

public abstract class LogRecordBuilder {
  private static final Logger LOGGER = LoggerFactory.getLogger(LogRecordBuilder.class);

  private static final String OKAPI_URL = "x-okapi-url";
  private static final String EXCEPTION_CALLING_ENDPOINT_MSG = "Exception calling {} {}";

  public static final String PERSONAL_NAME_PATTERN = "%s, %s";
  public static final String SEARCH_PARAMS = "?limit=%s&offset=%s%s";
  public static final String ID = "id";
  public static final String USERS = "users";

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
      httpClient.request(HttpMethod.GET, endpoint, okapiHeaders)
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

  /**
   * Returns list of item records for specified id's.
   *
   * @param ids List of item id's
   * @return future with list of item records
   */
  public CompletableFuture<List<JsonObject>> getEntitiesByIds(List<String> ids, String key) {
    String query = convertIdsToCqlQuery(ids);
    String endpoint = String.format(USERS_URL + SEARCH_PARAMS, 2, 0, buildQuery(query));
    return handleGetRequest(endpoint).thenApply(response -> extractEntities(response, key));
  }

  public CompletableFuture<JsonObject> fetchTemplateName(JsonObject payload) {
    return handleGetRequest(String.format(URL_WITH_ID_PATTERN, TEMPLATES_URL, getProperty(extractFirstItem(payload), TEMPLATE_ID)))
      .thenCompose(templateJson -> {
        if (nonNull(templateJson)) {
          return CompletableFuture.completedFuture(payload.put(TEMPLATE_NAME.value(),
              isNull(getProperty(templateJson, NAME)) ? EMPTY : getProperty(templateJson, NAME)));
        }
        return CompletableFuture.completedFuture(payload.put(TEMPLATE_NAME.value(), EMPTY));
      });
  }

  public CompletableFuture<JsonObject> fetchUserDetails(JsonObject payload, String userId) {
    return handleGetRequest(String.format(URL_WITH_ID_PATTERN, USERS_URL, userId))
      .thenCompose(userJson -> {
        if (nonNull(userJson)) {
          if (getProperty(payload, USER_ID).equals(userId)) {
            ofNullable(getProperty(userJson, BARCODE)).ifPresent(barcode -> payload.put(USER_BARCODE.value(), barcode));
          }
          JsonObject personal = getObjectProperty(userJson, PERSONAL);
          if (nonNull(personal)) {
            payload.put(PERSONAL_NAME.value(),
              String.format(PERSONAL_NAME_PATTERN, getProperty(personal, LAST_NAME), getProperty(personal, FIRST_NAME)));
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

  private String buildQuery(String query) {
    return isEmpty(query) ? StringUtils.EMPTY : "&query=" + encodeQuery(query);
  }

  /**
   * Validates if the json object contains entries and returns entries as list of JsonObject elements
   *
   * @param entries {@link JsonObject} representing item storage response
   * @return list of the entry records as JsonObject elements
   */
  private List<JsonObject> extractEntities(JsonObject entries, String key) {
    return ofNullable(entries.getJsonArray(key))
      .map(objects -> objects.stream()
        .map(entry -> (JsonObject) entry)
        .collect(toList()))
      .orElseGet(Collections::emptyList);
  }

  /**
   * @param query string representing CQL query
   * @return URL encoded string
   */
  private String encodeQuery(String query) {
    try {
      return URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException e) {
      LOGGER.error("Error happened while attempting to encode '{}'", query);
      throw new CompletionException(e);
    }
  }

  /**
   * Transform list of id's to CQL query using 'or' operation
   *
   * @param ids list of id's
   * @return String representing CQL query to get records by id's
   */
  private String convertIdsToCqlQuery(Collection<String> ids) {
    return convertIdsToCqlQuery(ids, ID, true);
  }

  /**
   * Transform list of values for some property to CQL query using 'or' operation
   *
   * @param values      list of field values
   * @param fieldName   the property name to search by
   * @param strictMatch indicates whether strict match mode (i.e. ==) should be used or not (i.e. =)
   * @return String representing CQL query to get records by some property values
   */
  private String convertIdsToCqlQuery(Collection<String> values, String fieldName, boolean strictMatch) {
    String prefix = fieldName + (strictMatch ? "==(" : "=(");
    return StreamEx.of(values)
      .joining(" or ", prefix, ")");
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

  protected LogRecord.Action resolveAction(String actionString) {
    try {
      return LogRecord.Action.fromValue(actionString);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Builder isn't implemented yet for: " + actionString);
    }
  }
}
