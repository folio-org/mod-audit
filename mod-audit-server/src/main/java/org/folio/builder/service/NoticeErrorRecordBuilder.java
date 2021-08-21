package org.folio.builder.service;

import static java.util.Optional.ofNullable;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.ERROR_MESSAGE;

import java.util.Map;

import org.folio.rest.jaxrs.model.LogRecord;

import io.vertx.core.Context;
import io.vertx.core.json.JsonObject;

public class NoticeErrorRecordBuilder extends AbstractNoticeRecordBuilder {
  public NoticeErrorRecordBuilder(Map<String, String> okapiHeaders, Context vertxContext) {
    super(okapiHeaders, vertxContext, LogRecord.Action.SEND_ERROR);
  }

  @Override
  protected String buildDescription(JsonObject payload, JsonObject itemJson) {
    return super.buildDescription(payload, itemJson) +
      " Error message: " + ofNullable(getProperty(payload, ERROR_MESSAGE)).orElse(UNKNOWN_VALUE);
  }

}
