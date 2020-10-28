package org.folio.builder.service;

import static org.folio.builder.LogRecordBuilderResolver.REQUEST_CANCELLED;
import static org.folio.builder.LogRecordBuilderResolver.REQUEST_CREATED;
import static org.folio.builder.LogRecordBuilderResolver.REQUEST_MOVED;
import static org.folio.builder.LogRecordBuilderResolver.REQUEST_REORDERED;
import static org.folio.builder.LogRecordBuilderResolver.REQUEST_UPDATED;
import static org.folio.util.JsonPropertyFetcher.getArrayProperty;
import static org.folio.util.JsonPropertyFetcher.getNestedObjectProperty;
import static org.folio.util.JsonPropertyFetcher.getNestedStringProperty;
import static org.folio.util.JsonPropertyFetcher.getObjectProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.BARCODE;
import static org.folio.util.LogEventPayloadField.CREATED;
import static org.folio.util.LogEventPayloadField.HOLDINGS_RECORD_ID;
import static org.folio.util.LogEventPayloadField.INSTANCE_ID;
import static org.folio.util.LogEventPayloadField.ITEM;
import static org.folio.util.LogEventPayloadField.ITEM_ID;
import static org.folio.util.LogEventPayloadField.LOAN_ID;
import static org.folio.util.LogEventPayloadField.LOG_EVENT_TYPE;
import static org.folio.util.LogEventPayloadField.ORIGINAL;
import static org.folio.util.LogEventPayloadField.PAYLOAD;
import static org.folio.util.LogEventPayloadField.REORDERED;
import static org.folio.util.LogEventPayloadField.REQUESTER;
import static org.folio.util.LogEventPayloadField.REQUESTS;
import static org.folio.util.LogEventPayloadField.REQUEST_ID;
import static org.folio.util.LogEventPayloadField.SERVICE_POINT_ID;
import static org.folio.util.LogEventPayloadField.STATUS;
import static org.folio.util.LogEventPayloadField.UPDATED;
import static org.folio.util.LogEventPayloadField.USER_ID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Context;
import org.folio.builder.description.RequestDescriptionBuilder;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.LinkToIds;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.util.JsonPropertyFetcher;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class RequestRecordBuilderService extends LogRecordBuilderService {

  public static final String CLOSED_CANCELLED_STATUS = "Closed - Cancelled";
  RequestDescriptionBuilder requestDescriptionBuilder = new RequestDescriptionBuilder();

  public RequestRecordBuilderService(Map<String, String> okapiHeaders, Context vertxContext) {
    super(okapiHeaders, vertxContext);
  }

  @Override
  public CompletableFuture<List<LogRecord>> buildLogRecord(JsonObject event) {
    return CompletableFuture.completedFuture(new ArrayList<>(buildRequestLogRecord(event)));
  }

  private List<LogRecord> buildRequestLogRecord(JsonObject event) {

    List<LogRecord> records = new ArrayList<>();
    LogRecord.Action action = resolveLogRecordAction(getProperty(event, LOG_EVENT_TYPE));

    LogRecord record = new LogRecord().withObject(LogRecord.Object.REQUEST);

    JsonObject requests = getNestedObjectProperty(event, PAYLOAD, REQUESTS);
    String servicePointId = getNestedStringProperty(event, PAYLOAD, SERVICE_POINT_ID);

    if (LogRecord.Action.CREATED == action) {
      JsonObject created = getObjectProperty(requests, CREATED);
      records.add(record.withAction(LogRecord.Action.CREATED)
        .withUserBarcode(getNestedStringProperty(requests, REQUESTER, BARCODE))
        .withServicePointId(servicePointId)
        .withItems(buildItems(created))
        .withDate(new Date())
        .withLinkToIds(new LinkToIds().withUserId(JsonPropertyFetcher.getProperty(created, USER_ID))
          .withRequestId(JsonPropertyFetcher.getProperty(created, REQUEST_ID)))
        .withDescription(requestDescriptionBuilder.buildCreateDescription(created)));

    } else if (LogRecord.Action.EDITED == action) {

      JsonObject original = getObjectProperty(requests, ORIGINAL);
      JsonObject updated = getObjectProperty(requests, UPDATED);

      String description;
      if (getProperty(updated, STATUS).equals(CLOSED_CANCELLED_STATUS)) {
        action = LogRecord.Action.CANCELLED;
        description = requestDescriptionBuilder.buildCancelledDescription(original, updated);
      } else {
        description = requestDescriptionBuilder.buildEditedDescription(original, updated);
      }

      records.add(record.withAction(action)
        .withUserBarcode(getNestedStringProperty(original, REQUESTER, BARCODE))
        .withServicePointId(servicePointId)
        .withItems(buildItems(original))
        .withDate(new Date())
        .withLinkToIds(new LinkToIds().withUserId(getProperty(original, USER_ID))
          .withRequestId(getProperty(original, REQUEST_ID)))
        .withDescription(description));

    } else if (LogRecord.Action.MOVED == action) {

      JsonObject original = getObjectProperty(requests, ORIGINAL);
      JsonObject updated = getObjectProperty(requests, UPDATED);

      records.add(record.withAction(LogRecord.Action.MOVED)
        .withUserBarcode(getNestedStringProperty(original, REQUESTER, BARCODE))
        .withServicePointId(servicePointId)
        .withItems(buildItems(original))
        .withDate(new Date())
        .withLinkToIds(new LinkToIds().withUserId(getProperty(original, USER_ID))
          .withRequestId(getProperty(original, REQUEST_ID)))
        .withDescription(requestDescriptionBuilder.buildMovedDescription(original, updated)));

    } else if (LogRecord.Action.QUEUE_POSITION_REORDERED == action) {

      JsonArray reordered = getArrayProperty(requests, REORDERED);

      LogRecord.Action efa = action;
      reordered.forEach(r -> {
        JsonObject o = (JsonObject) r;
        records.add(record.withAction(efa)
          .withUserBarcode(getNestedStringProperty(o, REQUESTER, BARCODE))
          .withServicePointId(servicePointId)
          .withItems(buildItems(o))
          .withDate(new Date())
          .withLinkToIds(new LinkToIds().withUserId(JsonPropertyFetcher.getProperty(o, USER_ID))
            .withRequestId(JsonPropertyFetcher.getProperty(o, REQUEST_ID)))
          .withDescription(requestDescriptionBuilder.buildReorderedDescription(o)));
      });
    } else {
      throw new IllegalArgumentException();
    }
    return records;
  }

  private List<Item> buildItems(JsonObject payload) {
    return Collections.singletonList(new Item().withItemId(getProperty(payload, ITEM_ID))
      .withItemBarcode(getNestedStringProperty(payload, ITEM, BARCODE))
      .withHoldingId(getProperty(payload, HOLDINGS_RECORD_ID))
      .withInstanceId(getProperty(payload, INSTANCE_ID))
      .withLoanId(getProperty(payload, LOAN_ID)));
  }

  private LogRecord.Action resolveLogRecordAction(String logEventType) {
    if (REQUEST_CREATED.equals(logEventType)) {
      return LogRecord.Action.CREATED;
    } else if (REQUEST_UPDATED.equals(logEventType)) {
      return LogRecord.Action.EDITED;
    } else if (REQUEST_MOVED.equals(logEventType)) {
      return LogRecord.Action.MOVED;
    } else if (REQUEST_REORDERED.equals(logEventType)) {
      return LogRecord.Action.QUEUE_POSITION_REORDERED;
    } else if (REQUEST_CANCELLED.equals(logEventType)) {
      return LogRecord.Action.CANCELLED;
    } else {
      throw new IllegalArgumentException("Builder isn't implemented yet for: " + logEventType);
    }
  }
}
