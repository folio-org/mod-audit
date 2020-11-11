package org.folio.builder.service;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;
import static org.folio.builder.LogRecordBuilderResolver.MANUAL_BLOCK_CREATED;
import static org.folio.builder.LogRecordBuilderResolver.MANUAL_BLOCK_DELETED;
import static org.folio.builder.LogRecordBuilderResolver.MANUAL_BLOCK_MODIFIED;
import static org.folio.util.Constants.USERS_URL;
import static org.folio.util.JsonPropertyFetcher.getNestedStringProperty;
import static org.folio.util.JsonPropertyFetcher.getObjectProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.BARCODE;
import static org.folio.util.LogEventPayloadField.FIRST_NAME;
import static org.folio.util.LogEventPayloadField.LAST_NAME;
import static org.folio.util.LogEventPayloadField.LOG_EVENT_TYPE;
import static org.folio.util.LogEventPayloadField.METADATA;
import static org.folio.util.LogEventPayloadField.PAYLOAD;
import static org.folio.util.LogEventPayloadField.PERSONAL;
import static org.folio.util.LogEventPayloadField.UPDATED_BY_USER_ID;
import static org.folio.util.LogEventPayloadField.USER_ID;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.folio.builder.description.ManualBlockDescriptionBuilder;
import org.folio.rest.jaxrs.model.LinkToIds;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.util.LogEventPayloadField;

import io.vertx.core.Context;
import io.vertx.core.json.JsonObject;
import one.util.streamex.StreamEx;

public class ManualBlockRecordBuilder extends LogRecordBuilder {
  public ManualBlockRecordBuilder(Map<String, String> okapiHeaders, Context vertxContext) {
    super(okapiHeaders, vertxContext);
  }

  @Override
  public CompletableFuture<List<LogRecord>> buildLogRecord(JsonObject event) {

    JsonObject payload = getObjectProperty(event, PAYLOAD);
    String logEventType = getProperty(event, LOG_EVENT_TYPE);

    String userId = getProperty(payload, USER_ID);
    String sourceId = getNestedStringProperty(payload, METADATA, UPDATED_BY_USER_ID);

    return getEntitiesByIds(USERS_URL, USERS, 2, 0, userId, sourceId).thenCompose(users -> {
      Map<String, JsonObject> usersGroupedById = StreamEx.of(users)
        .collect(toMap(u -> getProperty(u, LogEventPayloadField.ID), Function.identity()));
      LogRecord manualBlockLogRecord = buildManualBlockLogRecord(payload, logEventType, userId, sourceId, usersGroupedById);
      return CompletableFuture.completedFuture(singletonList(manualBlockLogRecord));
    });
  }

  private LogRecord buildManualBlockLogRecord(JsonObject payload, String logEventType, String userId, String sourceId,
      Map<String, JsonObject> usersGroupedById) {
    return new LogRecord().withObject(LogRecord.Object.MANUAL_BLOCK)
      .withUserBarcode(getProperty(usersGroupedById.get(userId), BARCODE))
      .withSource(getSource(logEventType, sourceId, usersGroupedById))
      .withAction(resolveLogRecordAction(logEventType))
      .withDate(new Date())
      .withDescription(new ManualBlockDescriptionBuilder().buildDescription(payload))
      .withLinkToIds(new LinkToIds().withUserId(getProperty(payload, USER_ID)));
  }

  private String getSource(String logEventType, String sourceId, Map<String, JsonObject> usersGroupedById) {
    JsonObject sourceJson = usersGroupedById.get(sourceId);
    return MANUAL_BLOCK_DELETED.equals(logEventType) ? null
        : buildPersonalName(getNestedStringProperty(sourceJson, PERSONAL, FIRST_NAME),
            getNestedStringProperty(sourceJson, PERSONAL, LAST_NAME));
  }

  private LogRecord.Action resolveLogRecordAction(String logEventType) {
    if (MANUAL_BLOCK_CREATED.equals(logEventType)) {
      return LogRecord.Action.CREATED;
    } else if (MANUAL_BLOCK_MODIFIED.equals(logEventType)) {
      return LogRecord.Action.MODIFIED;
    } else if (MANUAL_BLOCK_DELETED.equals(logEventType)) {
      return LogRecord.Action.DELETED;
    } else {
      throw new IllegalArgumentException("Builder isn't implemented yet for: " + logEventType);
    }
  }

}
