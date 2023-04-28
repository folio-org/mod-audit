package org.folio.builder.service;

import static java.util.Objects.isNull;
import static org.folio.builder.LogRecordBuilderResolver.REQUEST_CANCELLED;
import static org.folio.builder.LogRecordBuilderResolver.REQUEST_CREATED;
import static org.folio.builder.LogRecordBuilderResolver.REQUEST_CREATED_THROUGH_OVERRIDE;
import static org.folio.builder.LogRecordBuilderResolver.REQUEST_EXPIRED;
import static org.folio.builder.LogRecordBuilderResolver.REQUEST_MOVED;
import static org.folio.builder.LogRecordBuilderResolver.REQUEST_REORDERED;
import static org.folio.builder.LogRecordBuilderResolver.REQUEST_UPDATED;
import static org.folio.rest.jaxrs.model.LogRecord.Action.PICKUP_EXPIRED;
import static org.folio.util.Constants.CANCELLATION_REASONS_URL;
import static org.folio.util.Constants.SYSTEM;
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
import static org.folio.util.LogEventPayloadField.ITEM_BARCODE;
import static org.folio.util.LogEventPayloadField.ITEM_ID;
import static org.folio.util.LogEventPayloadField.LOAN_ID;
import static org.folio.util.LogEventPayloadField.LOG_EVENT_TYPE;
import static org.folio.util.LogEventPayloadField.METADATA;
import static org.folio.util.LogEventPayloadField.ORIGINAL;
import static org.folio.util.LogEventPayloadField.PAYLOAD;
import static org.folio.util.LogEventPayloadField.REORDERED;
import static org.folio.util.LogEventPayloadField.REQUESTER;
import static org.folio.util.LogEventPayloadField.REQUESTER_ID;
import static org.folio.util.LogEventPayloadField.REQUESTS;
import static org.folio.util.LogEventPayloadField.REQUEST_ID;
import static org.folio.util.LogEventPayloadField.REQUEST_PICKUP_SERVICE_POINT_ID;
import static org.folio.util.LogEventPayloadField.REQUEST_REASON_FOR_CANCELLATION_ID;
import static org.folio.util.LogEventPayloadField.SOURCE;
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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.builder.description.RequestDescriptionBuilder;
import org.folio.rest.external.CancellationReasonCollection;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.LinkToIds;
import org.folio.rest.jaxrs.model.LogRecord;

import io.vertx.core.Context;
import io.vertx.core.json.JsonObject;

public class RequestRecordBuilder extends LogRecordBuilder {

  private static final Logger LOGGER = LogManager.getLogger();

  public static final String CLOSED_CANCELLED_STATUS = "Closed - Cancelled";
  public static final String PICKUP_EXPIRED_STATUS = "Closed - Pickup expired";
  RequestDescriptionBuilder requestDescriptionBuilder = new RequestDescriptionBuilder();

  public RequestRecordBuilder(Map<String, String> okapiHeaders, Context vertxContext) {
    super(okapiHeaders, vertxContext);
  }

  @Override
  public CompletableFuture<List<LogRecord>> buildLogRecord(JsonObject event) {
    return buildRequestLogRecord(event);
  }

  private CompletableFuture<List<LogRecord>> buildRequestLogRecord(JsonObject event) {
    LOGGER.debug("buildRequestLogRecord:: Building Request Log Record");
    List<LogRecord> records = new ArrayList<>();
    final LogRecord.Action action = resolveLogRecordAction(getProperty(event, LOG_EVENT_TYPE));

    var requests = getNestedObjectProperty(event, PAYLOAD, REQUESTS);

    if (LogRecord.Action.CREATED == action || LogRecord.Action.CREATED_THROUGH_OVERRIDE == action) {
      LOGGER.info("buildRequestLogRecord:: Building log record for created action");
      var created = getObjectProperty(requests, CREATED);

      return fetchItemDetails(new JsonObject().put(ITEM_ID.value(), getProperty(created, ITEM_ID)))
        .thenCompose(item -> fetchUserAndSourceDetails(new JsonObject().put(USER_ID.value(), getProperty(created, REQUESTER_ID)),
            getProperty(created, REQUESTER_ID), getNestedStringProperty(created, METADATA, UPDATED_BY_USER_ID)).thenApply(user -> {
              records.add(buildBaseContent(created, item, user).withAction(action)
                .withDescription(requestDescriptionBuilder.buildCreateDescription(created)));
              return records;
            }));

    } else if (LogRecord.Action.EDITED == action) {
      LOGGER.info("buildRequestLogRecord:: Building log record for edited action");
      var original = getObjectProperty(requests, ORIGINAL);
      var updated = getObjectProperty(requests, UPDATED);

      return fetchItemDetails(new JsonObject().put(ITEM_ID.value(), getProperty(original, ITEM_ID)))
        .thenCompose(item -> fetchUserAndSourceDetails(new JsonObject().put(USER_ID.value(), getProperty(original, REQUESTER_ID)),
            getProperty(original, REQUESTER_ID), getNestedStringProperty(updated, METADATA, UPDATED_BY_USER_ID)).thenCompose(user -> {

              if (CLOSED_CANCELLED_STATUS.equals(getProperty(updated, STATUS))) {
                return getEntitiesByIds(CANCELLATION_REASONS_URL, CancellationReasonCollection.class, 1, 0,
                    getProperty(updated, REQUEST_REASON_FOR_CANCELLATION_ID)).thenApply(reasons -> {
                      records.add(buildBaseContent(updated, item, user).withAction(LogRecord.Action.CANCELLED)
                        .withDescription(requestDescriptionBuilder.buildCancelledDescription(original,
                          reasons.getCancellationReasons().get(0).getDescription())));
                      return records;
                    });
              } else {
                records.add(buildBaseContent(updated, item, user).withAction(action)
                  .withDescription(requestDescriptionBuilder.buildEditedDescription(original, updated)));
                return CompletableFuture.completedFuture(records);
              }
            }));

    } else if (LogRecord.Action.MOVED == action) {
      LOGGER.info("buildRequestLogRecord:: Building log record for moved action");

      var original = getObjectProperty(requests, ORIGINAL);
      var updated = getObjectProperty(requests, UPDATED);

      return fetchItemDetails(new JsonObject().put(ITEM_ID.value(), getProperty(original, ITEM_ID)))
        .thenCompose(item -> fetchUserAndSourceDetails(new JsonObject().put(USER_ID.value(), getProperty(updated, REQUESTER_ID)),
            getProperty(updated, REQUESTER_ID), getNestedStringProperty(updated, METADATA, UPDATED_BY_USER_ID)).thenApply(user -> {
              records.add(buildBaseContent(updated, item, user).withAction(LogRecord.Action.MOVED)
                .withDescription(requestDescriptionBuilder.buildMovedDescription(original, updated)));
              return records;
            }));

    } else if (LogRecord.Action.QUEUE_POSITION_REORDERED == action) {
      LOGGER.info("buildRequestLogRecord:: Building log record for Queue position reordered action");

      var reordered = getArrayProperty(requests, REORDERED);

      List<CompletableFuture<LogRecord>> pageContentFutures = reordered.stream()
        .map(req -> {
          var request = (JsonObject) req;
          return fetchItemDetails(new JsonObject().put(ITEM_ID.value(), getProperty(request, ITEM_ID)))
            .thenCompose(item -> fetchUserAndSourceDetails(new JsonObject().put(USER_ID.value(), getProperty(request, REQUESTER_ID)),
                getProperty(request, REQUESTER_ID), getNestedStringProperty(request, METADATA, UPDATED_BY_USER_ID))
                  .thenApply(user -> buildBaseContent(request, item, user)
                    .withUserBarcode(getNestedStringProperty(request, REQUESTER, BARCODE))
                    .withAction(action)
                    .withDescription(requestDescriptionBuilder.buildReorderedDescription(request))));
        })
        .collect(Collectors.toList());

      CompletableFuture<Void> allFutures = CompletableFuture.allOf(pageContentFutures.toArray(new CompletableFuture[0]));

      return allFutures.thenApply(v -> pageContentFutures.stream()
        .map(CompletableFuture::join)
        .collect(Collectors.toList()));

    } else if (LogRecord.Action.EXPIRED == action) {
      LOGGER.info("buildRequestLogRecord:: Building log record for expired action");

      var original = getObjectProperty(requests, ORIGINAL);
      var updated = getObjectProperty(requests, UPDATED);

      return fetchItemDetails(new JsonObject().put(ITEM_ID.value(), getProperty(original, ITEM_ID)))
        .thenCompose(item -> fetchUserAndSourceDetails(new JsonObject().put(USER_ID.value(), getProperty(original, REQUESTER_ID)),
            getProperty(original, REQUESTER_ID), getNestedStringProperty(updated, METADATA, UPDATED_BY_USER_ID)).thenApply(user -> {

              records.add(buildBaseContent(original, item, user).withSource(SYSTEM)
                .withAction(PICKUP_EXPIRED_STATUS.equals(getProperty(updated, STATUS)) ? PICKUP_EXPIRED : action)
                .withDescription(requestDescriptionBuilder.buildExpiredDescription(original, updated)));
              return records;
            }));

    } else {
      LOGGER.warn("Action isn't determined or invalid");
      throw new IllegalArgumentException("Action isn't determined or invalid");
    }
  }

  private LogRecord buildBaseContent(JsonObject created, JsonObject item, JsonObject user) {
    LOGGER.debug("buildBaseContent:: Building base content for LogRecord");
    return new LogRecord().withObject(LogRecord.Object.REQUEST)
      .withUserBarcode(getProperty(user, USER_BARCODE))
      .withServicePointId(getProperty(created, REQUEST_PICKUP_SERVICE_POINT_ID))
      .withItems(buildItemData(item))
      .withDate(new Date())
      .withLinkToIds(buildLinkToIds(created))
      .withSource(getProperty(user, SOURCE));
  }

  private LinkToIds buildLinkToIds(JsonObject created) {
    LOGGER.debug("buildLinkToIds:: Building LinkToIds for LogRecord.");
    return new LinkToIds().withUserId(getProperty(created, REQUESTER_ID))
      .withRequestId(getProperty(created, REQUEST_ID));
  }

  private List<Item> buildItemData(JsonObject payload) {
    LOGGER.debug("buildItemData:: Building item data for LogRecord.");
    return Collections.singletonList(new Item().withItemId(getProperty(payload, ITEM_ID))
      .withItemBarcode(isNull(getNestedStringProperty(payload, ITEM, BARCODE)) ? getProperty(payload, ITEM_BARCODE)
          : getNestedStringProperty(payload, ITEM, BARCODE))
      .withHoldingId(getProperty(payload, HOLDINGS_RECORD_ID))
      .withInstanceId(getProperty(payload, INSTANCE_ID))
      .withLoanId(getProperty(payload, LOAN_ID)));
  }

  private LogRecord.Action resolveLogRecordAction(String logEventType) {
    LOGGER.debug("resolveLogRecordAction:: Resolving LogRecord Action for event type : {}", logEventType);
    if (REQUEST_CREATED.equals(logEventType)) {
      LOGGER.info("resolveLogRecordAction::  LogRecord Action created");
      return LogRecord.Action.CREATED;
    } else if (REQUEST_CREATED_THROUGH_OVERRIDE.equals(logEventType)) {
      LOGGER.info("resolveLogRecordAction::  LogRecord Action created through override");
      return LogRecord.Action.CREATED_THROUGH_OVERRIDE;
    } else if (REQUEST_UPDATED.equals(logEventType)) {
      LOGGER.info("resolveLogRecordAction::  LogRecord Action updated");
      return LogRecord.Action.EDITED;
    } else if (REQUEST_MOVED.equals(logEventType)) {
      LOGGER.info("resolveLogRecordAction::  LogRecord Action moved");
      return LogRecord.Action.MOVED;
    } else if (REQUEST_REORDERED.equals(logEventType)) {
      LOGGER.info("resolveLogRecordAction::  LogRecord Action reordered");
      return LogRecord.Action.QUEUE_POSITION_REORDERED;
    } else if (REQUEST_CANCELLED.equals(logEventType)) {
      LOGGER.info("resolveLogRecordAction::  LogRecord Action cancelled");
      return LogRecord.Action.CANCELLED;
    } else if (REQUEST_EXPIRED.equals(logEventType)) {
      LOGGER.info("resolveLogRecordAction::  LogRecord Action expired");
      return LogRecord.Action.EXPIRED;
    } else {
      LOGGER.warn("Builder isn't implemented yet for: {}", logEventType);
      throw new IllegalArgumentException("Builder isn't implemented yet for: " + logEventType);
    }
  }
}
