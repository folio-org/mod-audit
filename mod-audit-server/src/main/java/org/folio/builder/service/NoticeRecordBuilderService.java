package org.folio.builder.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.folio.builder.description.Descriptions.NOTICE_MSG;
import static org.folio.rest.jaxrs.model.LogRecord.Object.NOTICE;
import static org.folio.util.Constants.SYSTEM;
import static org.folio.util.Constants.TEMPLATES_URL;
import static org.folio.util.Constants.URL_WITH_ID_PATTERN;
import static org.folio.util.Constants.UUID_PATTERN;
import static org.folio.util.JsonPropertyFetcher.getArrayProperty;
import static org.folio.util.JsonPropertyFetcher.getDateTimeProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.ACCOUNT_ID;
import static org.folio.util.LogEventPayloadField.DATE;
import static org.folio.util.LogEventPayloadField.HOLDINGS_RECORD_ID;
import static org.folio.util.LogEventPayloadField.INSTANCE_ID;
import static org.folio.util.LogEventPayloadField.ITEMS;
import static org.folio.util.LogEventPayloadField.ITEM_BARCODE;
import static org.folio.util.LogEventPayloadField.ITEM_ID;
import static org.folio.util.LogEventPayloadField.LOAN_ID;
import static org.folio.util.LogEventPayloadField.NAME;
import static org.folio.util.LogEventPayloadField.NOTICE_POLICY_ID;
import static org.folio.util.LogEventPayloadField.PAYLOAD;
import static org.folio.util.LogEventPayloadField.REQUEST_ID;
import static org.folio.util.LogEventPayloadField.SERVICE_POINT_ID;
import static org.folio.util.LogEventPayloadField.TEMPLATE_ID;
import static org.folio.util.LogEventPayloadField.TEMPLATE_NAME;
import static org.folio.util.LogEventPayloadField.TRIGGERING_EVENT;
import static org.folio.util.LogEventPayloadField.USER_BARCODE;
import static org.folio.util.LogEventPayloadField.USER_ID;

import io.vertx.core.Context;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.LinkToIds;
import org.folio.rest.jaxrs.model.LogRecord;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class NoticeRecordBuilderService extends LogRecordBuilderService {
  public NoticeRecordBuilderService(Map<String, String> okapiHeaders, Context vertxContext) {
    super(okapiHeaders, vertxContext);
  }

  @Override
  public CompletableFuture<List<LogRecord>> buildLogRecord(JsonObject fullPayload) {
    JsonObject payload = new JsonObject(getProperty(fullPayload, PAYLOAD));

    return fetchTemplateName(payload)
      .thenCompose(this::createResult);
  }

  private CompletableFuture<JsonObject> fetchTemplateName(JsonObject payload) {
    return handleGetRequest(String.format(URL_WITH_ID_PATTERN, TEMPLATES_URL, getProperty(extractFirstItem(payload), TEMPLATE_ID)))
      .thenCompose(templateJson -> {
        if (nonNull(templateJson)) {
          return CompletableFuture.completedFuture(payload
            .put(TEMPLATE_NAME.value(), isNull(getProperty(templateJson, NAME)) ? EMPTY : getProperty(templateJson, NAME)));
        }
        return CompletableFuture.completedFuture(payload.put(TEMPLATE_NAME.value(), EMPTY));
      });
  }

  private JsonObject extractFirstItem(JsonObject payload) {
    if (getArrayProperty(payload, ITEMS).isEmpty()) {
      return new JsonObject();
    }
    return getArrayProperty(payload, ITEMS).getJsonObject(0);
  }

  public List<Item> fetchItems(JsonArray itemsArray) {
    return itemsArray.stream()
      .map(itemJson -> createItem((JsonObject) itemJson))
      .collect(Collectors.toList());
  }

  private Item createItem(JsonObject itemJson) {
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
    return item;
  }

  private CompletableFuture<List<LogRecord>> createResult(JsonObject payload) {
    JsonObject itemJson = extractFirstItem(payload);
    LogRecord logRecord = new LogRecord()
      .withObject(NOTICE)
      .withUserBarcode(getProperty(payload, USER_BARCODE))
      .withItems(fetchItems(getArrayProperty(payload, ITEMS)))
      .withAction(LogRecord.Action.SEND)
      .withDate(getDateTimeProperty(payload, DATE).toDate())
      .withServicePointId(getProperty(extractFirstItem(payload), SERVICE_POINT_ID))
      .withSource(SYSTEM)
      .withLinkToIds(new LinkToIds()
        .withUserId(getProperty(payload, USER_ID))
        .withRequestId(getProperty(payload, REQUEST_ID))
        .withFeeFineId(getProperty(payload, ACCOUNT_ID))
        .withTemplateId(getProperty(itemJson, TEMPLATE_ID))
        .withNoticePolicyId(getProperty(itemJson, NOTICE_POLICY_ID)));

    logRecord.setDescription(String.format(NOTICE_MSG,
      getProperty(payload, TEMPLATE_NAME),
      ofNullable(getProperty(itemJson, TRIGGERING_EVENT)).orElse(EMPTY)));

    return CompletableFuture.completedFuture(Collections.singletonList(logRecord));
  }
}
