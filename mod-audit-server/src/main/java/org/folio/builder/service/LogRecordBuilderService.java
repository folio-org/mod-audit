package org.folio.builder.service;

import java.util.List;
import org.folio.rest.jaxrs.model.LogRecord;

import io.vertx.core.json.JsonObject;

public abstract class LogRecordBuilderService {
  public abstract List<LogRecord> buildLogRecord(JsonObject payload);
}
