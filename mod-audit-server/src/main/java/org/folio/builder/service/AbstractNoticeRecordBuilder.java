package org.folio.builder.service;

import static java.util.Optional.ofNullable;
import static org.folio.builder.description.Descriptions.NOTICE_MSG;
import static org.folio.rest.jaxrs.model.LogRecord.Object.NOTICE;
import static org.folio.util.Constants.SYSTEM;
import static org.folio.util.Constants.UUID_PATTERN;
import static org.folio.util.JsonPropertyFetcher.getArrayProperty;
import static org.folio.util.JsonPropertyFetcher.getObjectProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.ACCOUNT_ID;
import static org.folio.util.LogEventPayloadField.HOLDINGS_RECORD_ID;
import static org.folio.util.LogEventPayloadField.INSTANCE_ID;
import static org.folio.util.LogEventPayloadField.ITEMS;
import static org.folio.util.LogEventPayloadField.ITEM_BARCODE;
import static org.folio.util.LogEventPayloadField.ITEM_ID;
import static org.folio.util.LogEventPayloadField.LOAN_ID;
import static org.folio.util.LogEventPayloadField.NOTICE_POLICY_ID;
import static org.folio.util.LogEventPayloadField.PAYLOAD;
import static org.folio.util.LogEventPayloadField.REQUEST_ID;
import static org.folio.util.LogEventPayloadField.SERVICE_POINT_ID;
import static org.folio.util.LogEventPayloadField.TEMPLATE_ID;
import static org.folio.util.LogEventPayloadField.TEMPLATE_NAME;
import static org.folio.util.LogEventPayloadField.TRIGGERING_EVENT;
import static org.folio.util.LogEventPayloadField.USER_BARCODE;
import static org.folio.util.LogEventPayloadField.USER_ID;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.LinkToIds;
import org.folio.rest.jaxrs.model.LogRecord;

import io.vertx.core.Context;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public abstract class AbstractNoticeRecordBuilder extends LogRecordBuilder {

  private static final Logger LOGGER = LogManager.getLogger();
  protected static final String UNKNOWN_VALUE = "unknown";
  private final LogRecord.Action action;

  protected AbstractNoticeRecordBuilder(Map<String, String> okapiHeaders, Context vertxContext,
    LogRecord.Action action) {

    super(okapiHeaders, vertxContext);
    this.action = action;
  }

  @Override
  public CompletableFuture<List<LogRecord>> buildLogRecord(JsonObject fullPayload) {
    LOGGER.debug("buildLogRecord:: Building Log Record");
    JsonObject payload = getObjectProperty(fullPayload, PAYLOAD);

    LOGGER.info("buildLogRecord:: Log record created successfully");
    return fetchUserDetails(payload)
      .thenCompose(this::fetchTemplateName)
      .thenCompose(this::createResult);
  }

  private CompletableFuture<JsonObject> fetchUserDetails(JsonObject payload) {
    LOGGER.debug("fetchUserDetails:: Fetching User Details");
    String userBarcode = getProperty(payload, USER_BARCODE);

    LOGGER.info("fetchUserDetails:: Fetched User Details with user barcode : {}", userBarcode);
    return userBarcode != null
      ? fetchUserDetailsByUserBarcode(payload, userBarcode)
      : fetchUserDetails(payload, getProperty(payload, USER_ID));
  }

  private JsonObject extractFirstItem(JsonObject payload) {
    LOGGER.debug("extractFirstItem:: Extracting First Item");
    if (getArrayProperty(payload, ITEMS).isEmpty()) {
      LOGGER.info("extractFirstItem:: Failed Extarcting First Item because there are no items in payload");
      return new JsonObject();
    }
    LOGGER.info("extractFirstItem:: Extracted First Item");
    return getArrayProperty(payload, ITEMS).getJsonObject(0);
  }

  public List<Item> fetchItems(JsonArray itemsArray) {
    LOGGER.debug("fetchItems:: Fetching Items");
    return itemsArray.stream()
      .map(itemJson -> createItem((JsonObject) itemJson))
      .collect(Collectors.toList());
  }

  private Item createItem(JsonObject itemJson) {
    LOGGER.debug("createItem:: Creating Item");
    Item item = new Item()
      .withItemBarcode(getProperty(itemJson, ITEM_BARCODE))
      .withItemId(getProperty(itemJson, ITEM_ID))
      .withInstanceId(getProperty(itemJson, INSTANCE_ID))
      .withHoldingId(getProperty(itemJson, HOLDINGS_RECORD_ID));

    ofNullable(getProperty(itemJson, LOAN_ID)).ifPresent(id -> {
      if (id.matches(UUID_PATTERN)) {
        item.setLoanId(id);
      }
    });
    LOGGER.info("createItem:: Created Item");
    return item;
  }

  private CompletableFuture<List<LogRecord>> createResult(JsonObject payload) {
    LOGGER.debug("createResult:: Creating Result");
    JsonObject itemJson = extractFirstItem(payload);
    LogRecord logRecord = new LogRecord()
      .withObject(NOTICE)
      .withUserBarcode(getProperty(payload, USER_BARCODE))
      .withItems(fetchItems(getArrayProperty(payload, ITEMS)))
      .withAction(action)
      .withDate(new Date())
      .withServicePointId(getProperty(extractFirstItem(payload), SERVICE_POINT_ID))
      .withSource(SYSTEM)
      .withDescription(buildDescription(payload, itemJson))
      .withLinkToIds(new LinkToIds()
        .withUserId(getProperty(payload, USER_ID))
        .withRequestId(getProperty(payload, REQUEST_ID))
        .withFeeFineId(getProperty(payload, ACCOUNT_ID))
        .withTemplateId(getProperty(itemJson, TEMPLATE_ID))
        .withNoticePolicyId(getProperty(itemJson, NOTICE_POLICY_ID)));

    LOGGER.info("createResult:: Created Result");
    return CompletableFuture.completedFuture(Collections.singletonList(logRecord));
  }

  protected String buildDescription(JsonObject payload, JsonObject itemJson) {
    LOGGER.debug("buildDescription:: Building Description");
    return String.format(NOTICE_MSG,
      ofNullable(getProperty(payload, TEMPLATE_NAME)).orElse(UNKNOWN_VALUE),
      ofNullable(getProperty(itemJson, TRIGGERING_EVENT)).orElse(UNKNOWN_VALUE));
  }

}
