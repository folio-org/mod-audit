package org.folio.builder.service;

import static org.folio.builder.LogRecordBuilderResolver.MANUAL_BLOCK_CREATED;
import static org.folio.builder.LogRecordBuilderResolver.MANUAL_BLOCK_DELETED;
import static org.folio.builder.LogRecordBuilderResolver.MANUAL_BLOCK_MODIFIED;
import static org.folio.util.JsonPropertyFetcher.getObjectProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.LOG_EVENT_TYPE;
import static org.folio.util.LogEventPayloadField.PAYLOAD;
import static org.folio.util.LogEventPayloadField.USER_BARCODE;
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

import io.vertx.core.json.JsonObject;

public class ManualBlockRecordBuilder extends LogRecordBuilder {
  public ManualBlockRecordBuilder(Map<String, String> okapiHeaders, Context vertxContext) {
    super(okapiHeaders, vertxContext);
  }

  @Override
  public CompletableFuture<List<LogRecord>> buildLogRecord(JsonObject event) {

    JsonObject payload = getObjectProperty(event, PAYLOAD);
    String logEventType = getProperty(event, LOG_EVENT_TYPE);

    return fetchUserBarcode(payload)
      .thenCompose(p -> createResult(p, logEventType));
  }

  private CompletableFuture<List<LogRecord>> createResult(JsonObject payload, String logEventType) {
    List<LogRecord> logRecords = new ArrayList<>();

    LogRecord manualBlockLogRecord = new LogRecord().withObject(LogRecord.Object.MANUAL_BLOCK)
      .withUserBarcode(getProperty(payload, USER_BARCODE))
      .withAction(resolveLogRecordAction(logEventType))
      .withDate(new Date())
      .withDescription(new ManualBlockDescriptionBuilder().buildDescription(payload))
      .withLinkToIds(new LinkToIds().withUserId(getProperty(payload, USER_ID)));

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
