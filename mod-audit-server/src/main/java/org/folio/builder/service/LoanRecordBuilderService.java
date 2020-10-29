package org.folio.builder.service;

import static java.util.Objects.nonNull;
import static org.folio.rest.jaxrs.model.LogRecord.Object.LOAN;
import static org.folio.util.Constants.HOLDINGS_URL;
import static org.folio.util.Constants.ITEMS_URL;
import static org.folio.util.Constants.SYSTEM;
import static org.folio.util.Constants.URL_WITH_ID_PATTERN;
import static org.folio.util.Constants.USERS_URL;
import static org.folio.util.JsonPropertyFetcher.getDateTimeProperty;
import static org.folio.util.JsonPropertyFetcher.getObjectProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.ACTION;
import static org.folio.util.LogEventPayloadField.BARCODE;
import static org.folio.util.LogEventPayloadField.DATE;
import static org.folio.util.LogEventPayloadField.DESCRIPTION;
import static org.folio.util.LogEventPayloadField.FIRST_NAME;
import static org.folio.util.LogEventPayloadField.HOLDINGS_RECORD_ID;
import static org.folio.util.LogEventPayloadField.INSTANCE_ID;
import static org.folio.util.LogEventPayloadField.ITEM_BARCODE;
import static org.folio.util.LogEventPayloadField.ITEM_ID;
import static org.folio.util.LogEventPayloadField.LAST_NAME;
import static org.folio.util.LogEventPayloadField.LOAN_ID;
import static org.folio.util.LogEventPayloadField.PERSONAL;
import static org.folio.util.LogEventPayloadField.PERSONAL_NAME;
import static org.folio.util.LogEventPayloadField.SERVICE_POINT_ID;
import static org.folio.util.LogEventPayloadField.UPDATED_BY_USER_ID;
import static org.folio.util.LogEventPayloadField.USER_BARCODE;
import static org.folio.util.LogEventPayloadField.USER_ID;

import io.vertx.core.Context;
import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.LinkToIds;
import org.folio.rest.jaxrs.model.LogRecord;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class LoanRecordBuilderService extends LogRecordBuilderService {
  private static final String PERSONAL_NAME_PATTERN = "%s, %s";

  public LoanRecordBuilderService(Map<String, String> okapiHeaders, Context vertxContext) {
    super(okapiHeaders, vertxContext);
  }

  @Override
  public CompletableFuture<List<LogRecord>> buildLogRecord(JsonObject payload) {
    if (isAnonymize(payload)) {
      return fetchItemDetails(payload)
        .thenCompose(this::createResult);
    }
    return fetchPersonalName(payload)
      .thenCompose(this::createResult);
  }

  private CompletableFuture<List<LogRecord>> createResult(JsonObject payload) {
    return CompletableFuture.completedFuture(Collections.singletonList(new LogRecord()
      .withObject(LOAN)
      .withUserBarcode(getProperty(payload, USER_BARCODE))
      .withItems(Collections.singletonList(new Item()
        .withItemId(getProperty(payload, ITEM_ID))
        .withItemBarcode(getProperty(payload, ITEM_BARCODE))
        .withInstanceId(getProperty(payload, INSTANCE_ID))
        .withHoldingId(getProperty(payload, HOLDINGS_RECORD_ID))
        .withLoanId(getProperty(payload, LOAN_ID))))
      .withAction(LogRecord.Action.fromValue(getProperty(payload, ACTION)))
      .withDate(getDateTimeProperty(payload, DATE).toDate())
      .withServicePointId(getProperty(payload, SERVICE_POINT_ID))
      .withSource(isAnonymize(payload) ? SYSTEM : getProperty(payload, PERSONAL_NAME))
      .withDescription(getProperty(payload, DESCRIPTION))
      .withLinkToIds(new LinkToIds().withUserId(getProperty(payload, USER_ID)))));
  }

  private CompletableFuture<JsonObject> fetchPersonalName(JsonObject payload) {
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

  private CompletableFuture<JsonObject> fetchItemDetails(JsonObject payload) {
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

  private boolean isAnonymize(JsonObject payload) {
    return LogRecord.Action.ANONYMIZE.equals(LogRecord.Action.fromValue(getProperty(payload, ACTION)));
  }
}
