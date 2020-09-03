package org.folio.rest.handler;

import org.folio.rest.jaxrs.model.LogEventPayload;
import org.folio.rest.jaxrs.model.LogRecord;

import java.util.UUID;

public class LogEventProcessor {
  private LogEventProcessor() {
  }

  public static LogRecord processPayload(LogEventPayload payload) {
    return new LogRecord()
      .withEventId(UUID.randomUUID().toString())
      .withObject(LogRecord.Object.fromValue(payload.getLoggedObjectType().value()));
  }
}
