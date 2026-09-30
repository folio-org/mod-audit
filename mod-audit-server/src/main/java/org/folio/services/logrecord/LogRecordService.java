package org.folio.services.logrecord;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import java.util.Map;

public interface LogRecordService {

  /**
   * Builds, anonymizes (if applicable) and persists log record(s) for the given LOG_RECORD event payload.
   *
   * @param logEventType   the log event type, used to resolve the appropriate {@code LogRecordBuilder}
   * @param payload        the LOG_RECORD event payload
   * @param okapiHeaders   okapi headers (tenant, url, token) associated with the event
   * @param vertxContext   the Vert.x context
   * @return future that completes when the log record(s) have been saved
   */
  Future<Void> processLogRecord(String logEventType, JsonObject payload, Map<String, String> okapiHeaders,
    Context vertxContext);
}
