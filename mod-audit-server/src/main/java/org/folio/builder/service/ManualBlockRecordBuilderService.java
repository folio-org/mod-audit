package org.folio.builder.service;

import static org.folio.builder.LogRecordBuilderResolver.MANUAL_BLOCK_CREATED;
import static org.folio.builder.LogRecordBuilderResolver.MANUAL_BLOCK_DELETED;
import static org.folio.builder.LogRecordBuilderResolver.MANUAL_BLOCK_MODIFIED;
import static org.folio.util.JsonPropertyFetcher.getObjectProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.LOG_EVENT_TYPE;
import static org.folio.util.LogEventPayloadField.PAYLOAD;
import static org.folio.util.LogEventPayloadField.USER_ID;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Context;
import org.folio.builder.description.ManualBlockDescriptionBuilder;
import org.folio.rest.jaxrs.model.LinkToIds;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.util.JsonPropertyFetcher;

import io.vertx.core.json.JsonObject;

public class ManualBlockRecordBuilderService extends LogRecordBuilderService {
  public ManualBlockRecordBuilderService(Map<String, String> okapiHeaders, Context vertxContext) {
    super(okapiHeaders, vertxContext);
  }

  @Override
  public CompletableFuture<List<LogRecord>> buildLogRecord(JsonObject event) {
    List<LogRecord> logRecords = new ArrayList<>();

    String logEventType = getProperty(event, LOG_EVENT_TYPE);
    JsonObject manualBlockJson = getObjectProperty(event, PAYLOAD);

    LogRecord manualBlockLogRecord = new LogRecord().withObject(LogRecord.Object.MANUAL_BLOCK)
      .withAction(resolveLogRecordAction(logEventType))
      .withDate(new Date())
      .withDescription(new ManualBlockDescriptionBuilder().buildDescription(manualBlockJson))
      .withLinkToIds(new LinkToIds().withUserId(JsonPropertyFetcher.getProperty(manualBlockJson, USER_ID)));

    logRecords.add(manualBlockLogRecord);

    return CompletableFuture.completedFuture(logRecords);
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
