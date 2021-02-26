package org.folio.builder.service;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toMap;
import static org.folio.builder.LogRecordBuilderResolver.REQUEST_CANCELLED;
import static org.folio.builder.LogRecordBuilderResolver.REQUEST_CREATED;
import static org.folio.builder.LogRecordBuilderResolver.REQUEST_CREATED_THROUGH_OVERRIDE;
import static org.folio.builder.LogRecordBuilderResolver.REQUEST_EXPIRED;
import static org.folio.builder.LogRecordBuilderResolver.REQUEST_MOVED;
import static org.folio.builder.LogRecordBuilderResolver.REQUEST_REORDERED;
import static org.folio.builder.LogRecordBuilderResolver.REQUEST_UPDATED;
import static org.folio.util.Constants.CANCELLATION_REASONS_URL;
import static org.folio.util.Constants.SYSTEM;
import static org.folio.util.Constants.USERS_URL;
import static org.folio.util.JsonPropertyFetcher.getArrayProperty;
import static org.folio.util.JsonPropertyFetcher.getNestedObjectProperty;
import static org.folio.util.JsonPropertyFetcher.getNestedStringProperty;
import static org.folio.util.JsonPropertyFetcher.getObjectProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.BARCODE;
import static org.folio.util.LogEventPayloadField.CREATED;
import static org.folio.util.LogEventPayloadField.DESCRIPTION;
import static org.folio.util.LogEventPayloadField.FIRST_NAME;
import static org.folio.util.LogEventPayloadField.HOLDINGS_RECORD_ID;
import static org.folio.util.LogEventPayloadField.INSTANCE_ID;
import static org.folio.util.LogEventPayloadField.ITEM;
import static org.folio.util.LogEventPayloadField.ITEM_BARCODE;
import static org.folio.util.LogEventPayloadField.ITEM_ID;
import static org.folio.util.LogEventPayloadField.LAST_NAME;
import static org.folio.util.LogEventPayloadField.LOAN_ID;
import static org.folio.util.LogEventPayloadField.LOG_EVENT_TYPE;
import static org.folio.util.LogEventPayloadField.METADATA;
import static org.folio.util.LogEventPayloadField.ORIGINAL;
import static org.folio.util.LogEventPayloadField.PAYLOAD;
import static org.folio.util.LogEventPayloadField.PERSONAL;
import static org.folio.util.LogEventPayloadField.REORDERED;
import static org.folio.util.LogEventPayloadField.REQUESTER;
import static org.folio.util.LogEventPayloadField.REQUESTER_ID;
import static org.folio.util.LogEventPayloadField.REQUESTS;
import static org.folio.util.LogEventPayloadField.REQUEST_ID;
import static org.folio.util.LogEventPayloadField.REQUEST_PICKUP_SERVICE_POINT_ID;
import static org.folio.util.LogEventPayloadField.REQUEST_REASON_FOR_CANCELLATION_ID;
import static org.folio.util.LogEventPayloadField.STATUS;
import static org.folio.util.LogEventPayloadField.UPDATED;
import static org.folio.util.LogEventPayloadField.UPDATED_BY_USER_ID;
import static org.folio.util.LogEventPayloadField.USER_BARCODE;
import static org.folio.util.LogEventPayloadField.USER_ID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.folio.builder.description.RequestDescriptionBuilder;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.LinkToIds;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.util.LogEventPayloadField;

import io.vertx.core.Context;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import one.util.streamex.StreamEx;

public class RequestRecordBuilder extends LogRecordBuilder {

  public static final String CLOSED_CANCELLED_STATUS = "Closed - Cancelled";
  RequestDescriptionBuilder requestDescriptionBuilder = new RequestDescriptionBuilder();

  public RequestRecordBuilder(Map<String, String> okapiHeaders, Context vertxContext) {
    super(okapiHeaders, vertxContext);
  }

  @Override
  public CompletableFuture<List<LogRecord>> buildLogRecord(JsonObject event) {
    return buildRequestLogRecord(event);
  }

  private CompletableFuture<List<LogRecord>> buildRequestLogRecord(JsonObject event) {

    List<LogRecord> records = new ArrayList<>();
    final LogRecord.Action action = resolveLogRecordAction(getProperty(event, LOG_EVENT_TYPE));

    JsonObject requests = getNestedObjectProperty(event, PAYLOAD, REQUESTS);

    if (LogRecord.Action.CREATED == action || LogRecord.Action.CREATED_THROUGH_OVERRIDE == action) {
      JsonObject created = getObjectProperty(requests, CREATED);
      return getEntitiesByIds(USERS_URL, USERS, 1, 0, getSourceIdFromMetadata(created)).thenApply(sources -> {
        records.add(new LogRecord().withObject(LogRecord.Object.REQUEST)
          .withAction(action)
          .withUserBarcode(getNestedStringProperty(created, REQUESTER, BARCODE))
          .withServicePointId(getProperty(created, REQUEST_PICKUP_SERVICE_POINT_ID))
          .withItems(buildItems(created))
          .withDate(new Date())
          .withSource(buildSourceName(sources.get(0)))
          .withLinkToIds(buildLinkToIds(created))
          .withDescription(requestDescriptionBuilder.buildCreateDescription(created)));
        return records;
      });

    } else if (LogRecord.Action.EDITED == action) {

      JsonObject original = getObjectProperty(requests, ORIGINAL);
      JsonObject updated = getObjectProperty(requests, UPDATED);

      return getEntitiesByIds(USERS_URL, USERS, 1, 0, getSourceIdFromMetadata(original))
        .thenCompose(sources -> {

        LogRecord record = new LogRecord().withObject(LogRecord.Object.REQUEST);

        if (CLOSED_CANCELLED_STATUS.equals(getProperty(updated, STATUS))) {
          return getEntitiesByIds(CANCELLATION_REASONS_URL, CANCELLATION_REASONS, 1, 0, getProperty(updated, REQUEST_REASON_FOR_CANCELLATION_ID))
            .thenApply(reasons -> {
              record.setAction(LogRecord.Action.CANCELLED);
              String description = requestDescriptionBuilder.buildCancelledDescription(original, getProperty(reasons.get(0), DESCRIPTION));

              records.add(record.withUserBarcode(getNestedStringProperty(original, REQUESTER, BARCODE))
                .withServicePointId(getProperty(updated, REQUEST_PICKUP_SERVICE_POINT_ID))
                .withItems(buildItems(original))
                .withDate(new Date())
                .withSource(buildSourceName(sources.get(0)))
                .withLinkToIds(buildLinkToIds(updated))
                .withDescription(description));
              return records;
            });
        } else {
          record.setAction(action);
          String description = requestDescriptionBuilder.buildEditedDescription(original, updated);

          records.add(record.withUserBarcode(getNestedStringProperty(original, REQUESTER, BARCODE))
            .withServicePointId(getProperty(updated, REQUEST_PICKUP_SERVICE_POINT_ID))
            .withItems(buildItems(original))
            .withDate(new Date())
            .withSource(buildSourceName(sources.get(0)))
            .withLinkToIds(buildLinkToIds(updated))
            .withDescription(description));

          return CompletableFuture.completedFuture(records);
        }
     });

    } else if (LogRecord.Action.MOVED == action) {

      JsonObject original = getObjectProperty(requests, ORIGINAL);
      JsonObject updated = getObjectProperty(requests, UPDATED);

      return getEntitiesByIds(USERS_URL, USERS, 1, 0, getSourceIdFromMetadata(original))
        .thenApply(sources -> {

        records.add(new LogRecord().withObject(LogRecord.Object.REQUEST)
          .withAction(LogRecord.Action.MOVED)
          .withUserBarcode(getNestedStringProperty(updated, REQUESTER, BARCODE))
          .withServicePointId(getProperty(updated, REQUEST_PICKUP_SERVICE_POINT_ID))
          .withItems(buildItems(updated))
          .withDate(new Date())
          .withSource(buildSourceName(sources.get(0)))
          .withLinkToIds(buildLinkToIds(updated))
          .withDescription(requestDescriptionBuilder.buildMovedDescription(original, updated)));
        return records;
      });

    } else if (LogRecord.Action.QUEUE_POSITION_REORDERED == action) {

      JsonArray reordered = getArrayProperty(requests, REORDERED);

      List<String> sourceIds = new ArrayList<>();
      for (int i = 0; i < reordered.size(); i++) {
        sourceIds.add(getSourceIdFromMetadata(reordered.getJsonObject(i)));
      }

      String[] sources = new String[sourceIds.size()];
      sourceIds.toArray(sources);

      return getEntitiesByIds(USERS_URL, USERS, sources.length, 0, sources).thenApply(s -> {

        Map<String, String> usersGroupedById = StreamEx.of(s)
          .collect(toMap(u -> getProperty(u, LogEventPayloadField.ID),
            v -> buildPersonalName(getNestedStringProperty(v, PERSONAL, FIRST_NAME),
              getNestedStringProperty(v, PERSONAL, LAST_NAME))));

        reordered.forEach(request -> {
          JsonObject r = (JsonObject) request;
          records.add(new LogRecord().withObject(LogRecord.Object.REQUEST)
            .withUserBarcode(getNestedStringProperty(r, REQUESTER, BARCODE))
            .withServicePointId(getProperty(r, REQUEST_PICKUP_SERVICE_POINT_ID))
            .withItems(buildItems(r))
            .withDate(new Date())
            .withAction(action)
            .withSource(usersGroupedById.get(getSourceIdFromMetadata(r)))
            .withLinkToIds(buildLinkToIds(r))
            .withDescription(requestDescriptionBuilder.buildReorderedDescription(r)));
        });
        return records;
      });
    } else if (LogRecord.Action.EXPIRED == action) {

      JsonObject original = getObjectProperty(requests, ORIGINAL);
      JsonObject updated = getObjectProperty(requests, UPDATED);

      return fetchItemDetails(new JsonObject().put(ITEM_ID.value(), getProperty(original, ITEM_ID)))
        .thenCompose(itemJson -> fetchUserDetails(new JsonObject()
          .put(USER_ID.value(), getProperty(original, REQUESTER_ID)), getProperty(original, REQUESTER_ID))
        .thenApply(userJson -> {

          records.add(new LogRecord().withObject(LogRecord.Object.REQUEST)
            .withUserBarcode(getProperty(userJson, USER_BARCODE))
            .withItems(buildItems(itemJson))
            .withDate(new Date())
            .withAction(action)
            .withSource(SYSTEM)
            .withLinkToIds(buildLinkToIds(original))
            .withDescription(requestDescriptionBuilder.buildExpiredDescription(original, updated)));
          return records;
        }));

    } else {
      throw new IllegalArgumentException("Action isn't determined or invalid");
    }
  }

  private LinkToIds buildLinkToIds(JsonObject created) {
    return new LinkToIds().withUserId(getProperty(created, REQUESTER_ID))
      .withRequestId(getProperty(created, REQUEST_ID));
  }

  /**
   * This method extracts sourceId from metadata
   *
   * @param json - JSON object
   * @return id of source (id of user that editing object)
   */
  private String getSourceIdFromMetadata(JsonObject json) {
    return getNestedStringProperty(json, METADATA, UPDATED_BY_USER_ID);
  }

  /**
   * This method builds name of source
   *
   * @param personal JSON object source personal
   * @return name of source (user that editing object)
   */
  private String buildSourceName(JsonObject personal) {
    String source = null;
    if (Objects.nonNull(personal)) {
      source = buildPersonalName(getNestedStringProperty(personal, PERSONAL, FIRST_NAME),
          getNestedStringProperty(personal, PERSONAL, LAST_NAME));
    }
    return source;
  }

  private List<Item> buildItems(JsonObject payload) {
    return Collections.singletonList(new Item().withItemId(getProperty(payload, ITEM_ID))
      .withItemBarcode(isNull(getNestedStringProperty(payload, ITEM, BARCODE)) ? getProperty(payload, ITEM_BARCODE) : getNestedStringProperty(payload, ITEM, BARCODE))
      .withHoldingId(getProperty(payload, HOLDINGS_RECORD_ID))
      .withInstanceId(getProperty(payload, INSTANCE_ID))
      .withLoanId(getProperty(payload, LOAN_ID)));
  }

  private LogRecord.Action resolveLogRecordAction(String logEventType) {
    if (REQUEST_CREATED.equals(logEventType)) {
      return LogRecord.Action.CREATED;
    } else if (REQUEST_CREATED_THROUGH_OVERRIDE.equals(logEventType)) {
      return LogRecord.Action.CREATED_THROUGH_OVERRIDE;
    } else if (REQUEST_UPDATED.equals(logEventType)) {
      return LogRecord.Action.EDITED;
    } else if (REQUEST_MOVED.equals(logEventType)) {
      return LogRecord.Action.MOVED;
    } else if (REQUEST_REORDERED.equals(logEventType)) {
      return LogRecord.Action.QUEUE_POSITION_REORDERED;
    } else if (REQUEST_CANCELLED.equals(logEventType)) {
      return LogRecord.Action.CANCELLED;
    } else if (REQUEST_EXPIRED.equals(logEventType)) {
      return LogRecord.Action.EXPIRED;
    } else {
      throw new IllegalArgumentException("Builder isn't implemented yet for: " + logEventType);
    }
  }
}
