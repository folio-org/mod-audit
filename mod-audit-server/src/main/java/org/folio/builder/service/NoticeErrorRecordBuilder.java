package org.folio.builder.service;

import static java.util.Optional.ofNullable;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.ERROR_MESSAGE;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.LogRecord;

import io.vertx.core.Context;
import io.vertx.core.json.JsonObject;

public class NoticeErrorRecordBuilder extends AbstractNoticeRecordBuilder {
  private static final Logger LOGGER = LogManager.getLogger();

  public NoticeErrorRecordBuilder(Map<String, String> okapiHeaders, Context vertxContext) {
    super(okapiHeaders, vertxContext, LogRecord.Action.SEND_ERROR);
  }

  @Override
  protected String buildDescription(JsonObject payload, JsonObject itemJson) {
    LOGGER.debug("buildDescription:: Building description with payload and item json");
    return super.buildDescription(payload, itemJson) +
      " Error message: " + ofNullable(getProperty(payload, ERROR_MESSAGE)).orElse(UNKNOWN_VALUE);
  }

}
