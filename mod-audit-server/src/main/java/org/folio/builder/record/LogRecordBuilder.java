package org.folio.builder.record;

import java.util.List;

import org.folio.rest.jaxrs.model.LogRecord;

import io.vertx.core.json.JsonObject;

public interface LogRecordBuilder {
  List<LogRecord> buildLogRecord(JsonObject payload);
}
