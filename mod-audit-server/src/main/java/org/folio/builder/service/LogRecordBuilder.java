package org.folio.builder.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.util.Constants.CIRCULATION_ITEM_URL;
import static org.folio.util.Constants.HOLDINGS_URL;
import static org.folio.util.Constants.ITEMS_URL;
import static org.folio.util.Constants.TEMPLATES_URL;
import static org.folio.util.Constants.URL_WITH_ID_PATTERN;
import static org.folio.util.Constants.USERS_URL;
import static org.folio.util.JsonPropertyFetcher.getArrayProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.BARCODE;
import static org.folio.util.LogEventPayloadField.HOLDINGS_RECORD_ID;
import static org.folio.util.LogEventPayloadField.INSTANCE_ID;
import static org.folio.util.LogEventPayloadField.ITEMS;
import static org.folio.util.LogEventPayloadField.ITEM_BARCODE;
import static org.folio.util.LogEventPayloadField.ITEM_ID;
import static org.folio.util.LogEventPayloadField.NAME;
import static org.folio.util.LogEventPayloadField.PERSONAL_NAME;
import static org.folio.util.LogEventPayloadField.SOURCE;
import static org.folio.util.LogEventPayloadField.TEMPLATE_ID;
import static org.folio.util.LogEventPayloadField.TEMPLATE_NAME;
import static org.folio.util.LogEventPayloadField.USER_BARCODE;
import static org.folio.util.LogEventPayloadField.USER_ID;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.external.User;
import org.folio.rest.external.UserCollection;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.Response;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.Context;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import one.util.streamex.StreamEx;

public abstract class LogRecordBuilder {
  private static final Logger LOGGER = LogManager.getLogger();

  private static final String OKAPI_URL = "x-okapi-url";
  private static final String EXCEPTION_CALLING_ENDPOINT_MSG = "Exception calling {} {}";

  public static final String SEARCH_PARAMS = "?limit=%s&offset=%s%s";
  public static final String ID = "id";

  protected final Map<String, String> okapiHeaders;
  protected final Context vertxContext;
  protected final String logEventType;

  public abstract CompletableFuture<List<LogRecord>> buildLogRecord(JsonObject payload);

  public LogRecordBuilder(Map<String, String> okapiHeaders, Context vertxContext) {
    this.okapiHeaders = okapiHeaders;
    this.vertxContext = vertxContext;
    this.logEventType = null;
  }

  public LogRecordBuilder(Map<String, String> okapiHeaders, Context vertxContext, String logEventType) {
    this.okapiHeaders = okapiHeaders;
    this.vertxContext = vertxContext;
    this.logEventType = logEventType;
  }

  private CompletableFuture<JsonObject> handleGetRequest(String endpoint) {
    LOGGER.debug("handleGetRequest:: handling Get Request with endpoint : {}", endpoint);
    CompletableFuture<JsonObject> future = new CompletableFuture<>();

    final String okapiURL = okapiHeaders.getOrDefault(OKAPI_URL, "");
    final String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
    HttpClientInterface httpClient = HttpClientFactory.getHttpClient(okapiURL, tenantId);

    try {
      LOGGER.debug("handleGetRequest::Calling GET {}", endpoint);
      httpClient.request(HttpMethod.GET, endpoint, okapiHeaders)
        .whenComplete((response, throwable) -> {
          if (Objects.nonNull(throwable)) {
            LOGGER.error(EXCEPTION_CALLING_ENDPOINT_MSG, HttpMethod.GET, endpoint);
            future.completeExceptionally(throwable);
          } else {
            future.complete(verifyAndExtractBody(response));
          }
          httpClient.closeClient();
        });
    } catch (Exception e) {
      LOGGER.warn(EXCEPTION_CALLING_ENDPOINT_MSG, HttpMethod.GET, endpoint);
      future.completeExceptionally(e);
      httpClient.closeClient();
    }
    return future;
  }

  public <T> CompletableFuture<T> getEntitiesByQuery(String url, Class<T> collection, int limit, int offset, String query) {
    LOGGER.debug("getEntitiesByQuery:: Getting Entities By Query : {}", query);
    String endpoint = String.format(url + SEARCH_PARAMS, limit, offset, buildQuery(query));
    LOGGER.debug("getEntitiesByQuery:: Entities successfully retrieved from endpoint: {}", endpoint);
    return handleGetRequest(endpoint).thenApply(response -> response.mapTo(collection));
  }

  /**
   * Returns list of item records for specified id's.
   *
   * @param ids List of item id's
   * @return future with list of item records
   */
  public <T> CompletableFuture<T> getEntitiesByIds(String url, Class<T> collection, int limit, int offset, String... ids) {
    LOGGER.debug("getEntitiesByIds:: Getting Entities By Ids");
    return getEntitiesByQuery(url, collection, limit, offset, convertIdsToCqlQuery(ids));
  }

  public CompletableFuture<JsonObject> fetchTemplateName(JsonObject payload) {
    LOGGER.debug("fetchTemplateName:: Fetching Template Name");
    String templateId = getProperty(extractFirstItem(payload), TEMPLATE_ID);

    if (templateId == null) {
      LOGGER.warn("No template ID found in payload, returning payload as is.");
      return completedFuture(payload);
    }
    LOGGER.info("fetchTemplateName:: Fetching template name from URL");
    return handleGetRequest(String.format(URL_WITH_ID_PATTERN, TEMPLATES_URL, templateId))
      .thenCompose(templateJson -> {
        if (nonNull(templateJson)) {
          LOGGER.info("fetchTemplateName:: Template name successfully retrieved");
          return completedFuture(payload.put(TEMPLATE_NAME.value(),
              isNull(getProperty(templateJson, NAME)) ? EMPTY : getProperty(templateJson, NAME)));
        }
        LOGGER.warn("No template found with ID: {}", templateId);
        return completedFuture(payload.put(TEMPLATE_NAME.value(), EMPTY));
      });
  }

  public CompletableFuture<JsonObject> fetchUserDetails(JsonObject payload, String userId) {
    LOGGER.debug("fetchUserDetails:: Fetching user details for user Id : {}", userId);
    return getEntitiesByIds(USERS_URL, UserCollection.class, 1, 0, userId)
      .thenCompose(users -> {
        users.getUsers()
          .stream()
          .findFirst()
          .ifPresent(user -> updatePayload(payload, userId, user));
        LOGGER.debug("fetchUserDetails:: Fetched user details for user Id : {}", userId);
        return CompletableFuture.completedFuture(payload);
      });
  }

  private void updatePayload(JsonObject payload, String userId, User user) {
    LOGGER.debug("updatePayload:: Updating payload with details for user ID {}", userId);
    if (nonNull(user)) {
      if (userId.equals(getProperty(payload, USER_ID))) {
        LOGGER.debug("updatePayload:: Updating user id to payload because user id : {} matched with user id from payload", userId);
        payload.put(USER_BARCODE.value(), user.getBarcode());
      }
      fetchUserPersonal(payload, user);
    }
  }

  public CompletableFuture<JsonObject> fetchUserAndSourceDetails(JsonObject payload, String userId, String sourceId) {
    LOGGER.debug("fetchUserAndSourceDetails:: Fetching user details for user Id : {} and source ID {}", userId, sourceId);
    return getEntitiesByIds(USERS_URL, UserCollection.class, 2, 0, userId, sourceId)
      .thenCompose(users -> {
      Map<String, User> usersGroupedById = StreamEx.of(users.getUsers()).collect(toMap(User::getId, identity()));

        var user = usersGroupedById.get(userId);
        var source = usersGroupedById.get(sourceId);
        updatePayload(payload, userId, user);

        if (nonNull(source)) {
          LOGGER.info("fetchUserAndSourceDetails:: Adding source to the payload");
          payload.put(SOURCE.value(),
            buildPersonalName(source.getPersonal().getFirstName(), source.getPersonal().getLastName()));
        }
        LOGGER.info("fetchUserAndSourceDetails:: Fetched user details for user Id : {} and source ID {}", userId, sourceId);
        return completedFuture(payload);
    });
  }

  public CompletableFuture<JsonObject> fetchUserDetailsByUserBarcode(JsonObject payload, String userBarcode) {
    LOGGER.debug("fetchUserDetailsByUserBarcode:: Fetching user details by user barcode {}", userBarcode);
    return getEntitiesByQuery(USERS_URL, UserCollection.class, 1, 0, "barcode==" + userBarcode)
      .thenCompose(users -> {
        var user = users.getUsers().get(0);
        if (nonNull(user)) {
          if (userBarcode.equals(getProperty(payload, USER_BARCODE))) {
            LOGGER.info("fetchUserDetailsByUserBarcode:: Adding user id to payload because user barcode : {} matched with user barcode from payload", userBarcode);
            payload.put(USER_ID.value(), user.getId());
          }
          fetchUserPersonal(payload, user);
        }
        LOGGER.info("fetchUserDetailsByUserBarcode:: Fetched user details by user barcode {}", userBarcode);
        return completedFuture(payload);
      });
  }

  private void fetchUserPersonal(JsonObject payload, User user) {
    LOGGER.debug("fetchUserPersonal:: Fetching User Personal");
    var personal = user.getPersonal();
    if (nonNull(personal) && nonNull(buildPersonalName(personal.getFirstName(), personal.getLastName()))) {
      LOGGER.info("fetchUserPersonal:: Fetched personal name");
      payload.put(PERSONAL_NAME.value(),
        buildPersonalName(personal.getFirstName(), personal.getLastName()));
    }
  }

  public CompletableFuture<JsonObject> fetchItemDetails(JsonObject payload) {
    LOGGER.debug("fetchItemDetails:: Fetching Item Details");
    return handleGetRequest(String.format(URL_WITH_ID_PATTERN, ITEMS_URL, getProperty(payload, ITEM_ID)))
      .thenCompose(itemJson -> nonNull(itemJson) ? CompletableFuture.completedFuture(itemJson) :
        handleGetRequest(String.format(URL_WITH_ID_PATTERN, CIRCULATION_ITEM_URL, getProperty(payload, ITEM_ID))))
      .thenCompose(itemJson -> addItemData(payload, itemJson))
      .thenCompose(json ->
        handleGetRequest(String.format(URL_WITH_ID_PATTERN, HOLDINGS_URL, getProperty(json, HOLDINGS_RECORD_ID))))
      .thenCompose(holdingJson -> addHoldingData(payload, holdingJson));
  }

  private String buildQuery(String query) {
    return isEmpty(query) ? StringUtils.EMPTY : "&query=" + encodeQuery(query);
  }

  /**
   * @param query string representing CQL query
   * @return URL encoded string
   */
  private String encodeQuery(String query) {
    LOGGER.debug("encodeQuery:: Encoding Query : {}", query );
    try {
      LOGGER.debug("Encoded query");
      return URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException e) {
      LOGGER.warn("Error happened while attempting to encode '{}'", query);
      throw new CompletionException(e);
    }
  }

  /**
   * Transform list of id's to CQL query using 'or' operation
   *
   * @param ids list of id's
   * @return String representing CQL query to get records by id's
   */
  private String convertIdsToCqlQuery(String... ids) {
    return convertIdsToCqlQuery(ID, true, ids);
  }

  /**
   * Transform list of values for some property to CQL query using 'or' operation
   *
   * @param values      list of field values
   * @param fieldName   the property name to search by
   * @param strictMatch indicates whether strict match mode (i.e. ==) should be used or not (i.e. =)
   * @return String representing CQL query to get records by some property values
   */
  private String convertIdsToCqlQuery(String fieldName, boolean strictMatch, String... values) {
    LOGGER.debug("convertIdsToCqlQuery:: Converting Ids To CqlQuery with field name : {}", fieldName);
    String prefix = fieldName + (strictMatch ? "==(" : "=(");
    LOGGER.debug("Converted IDs to CQL query");
    return StreamEx.of(values)
      .joining(" or ", prefix, ")");
  }

  private CompletableFuture<JsonObject> addItemData(JsonObject payload, JsonObject itemJson) {
    LOGGER.debug("addItemData:: Adding Item Data");
    if (nonNull(itemJson)) {
      ofNullable(getProperty(itemJson, BARCODE))
        .ifPresent(barcode -> payload.put(ITEM_BARCODE.value(), barcode));
      LOGGER.debug("addItemData:: Added item barcode");
      ofNullable(getProperty(itemJson, HOLDINGS_RECORD_ID))
        .ifPresent(holdingsRecordId -> payload.put(HOLDINGS_RECORD_ID.value(), holdingsRecordId));
      LOGGER.debug("addItemData:: Added holdings record ID");
    }
    LOGGER.info("addItemData:: Added Item Data");
    return completedFuture(payload);
  }

  private CompletableFuture<JsonObject> addHoldingData(JsonObject payload, JsonObject holdingJson) {
    LOGGER.debug("addHoldingData:: Adding Holding Data");
    if (nonNull(holdingJson)) {
      ofNullable(getProperty(holdingJson, INSTANCE_ID))
        .ifPresent(instanceId -> payload.put(INSTANCE_ID.value(), instanceId));
    }
    LOGGER.info("addHoldingData:: Added Holding Data");
    return completedFuture(payload);
  }

  private JsonObject extractFirstItem(JsonObject payload) {
    LOGGER.debug("extractFirstItem:: Extracting First Item from payload");
    if (getArrayProperty(payload, ITEMS).isEmpty()) {
      LOGGER.warn("No items found in payload");
      return new JsonObject();
    }
    LOGGER.debug("extractFirstItem:: Extracted First Item from payload");
    return getArrayProperty(payload, ITEMS).getJsonObject(0);
  }

  private static JsonObject verifyAndExtractBody(Response response) {
    LOGGER.debug("verifyAndExtractBody:: Verifying and Extracting Body");
    var endpoint = response.getEndpoint();
    var code = response.getCode();
    var body = response.getBody();
    if (!Response.isSuccess(code)) {
      LOGGER.warn("Error calling {} with code {}, response body: {}", endpoint, code, body);
      return null;
    }
    LOGGER.debug("verifyAndExtractBody:: The response body for GET {}: {}", endpoint, nonNull(body) ? body.encodePrettily() : null);
    LOGGER.info("verifyAndExtractBody:: Verifying and Extracting Body completed");
    return response.getBody();
  }

  protected LogRecord.Action resolveAction(String actionString) {
    LOGGER.debug("resolveAction:: Resolving Action");
    try {
      LOGGER.debug("resolveAction:: Trying to Resolve Action with action String : {}", actionString);
      return LogRecord.Action.fromValue(actionString);
    } catch (IllegalArgumentException e) {
      String errorMessage = "Builder isn't implemented yet for: " + actionString;
      if (isEmpty(actionString)) {
        errorMessage = "Action is empty";
        LOGGER.warn(errorMessage);
      }
      throw new IllegalArgumentException(errorMessage);
    }
  }

  String buildPersonalName(String firstName, String lastName) {
    LOGGER.debug("buildPersonalName:: Building Personal Name with firstname : {} and lastname : {}", firstName, lastName);
    if (isNotEmpty(firstName) && isNotEmpty(lastName)) {
      LOGGER.debug("buildPersonalName:: Built Personal Name with firstname : {} and lastname : {}", firstName, lastName);
      return lastName + ", " + firstName;
    } else if (isEmpty(firstName) && isNotEmpty(lastName)) {
      LOGGER.debug("buildPersonalName:: Built Personal Name with lastname : {}", lastName);
      return lastName;
    } else if (isNotEmpty(firstName) && isEmpty(lastName)) {
      LOGGER.debug("buildPersonalName:: Built Personal Name with firstname : {}", firstName);
      return firstName;
    } else {
      LOGGER.debug("buildPersonalName:: Error building personal name because there is no firstname and lastname");
      return null;
    }
  }
}
