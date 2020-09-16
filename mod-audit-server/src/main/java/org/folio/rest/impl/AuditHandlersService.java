package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.impl.CirculationLogsService.LOGS_TABLE_NAME;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.handler.LogEventProcessor;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.LogEventPayload;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.rest.jaxrs.resource.AuditHandlers;
import org.folio.rest.tools.utils.ObjectMapperTool;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class AuditHandlersService extends BaseService implements AuditHandlers {

  private static final ObjectMapper MAPPER = ObjectMapperTool.getMapper();
  private static final Logger LOGGER = LoggerFactory.getLogger(AuditHandlersService.class);

  @Override
  @Validate
  public void postAuditHandlersLogRecord(String entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
      LogEventPayload logEventPayload = MAPPER.readValue(entity, LogEventPayload.class);
      LogRecord logRecord = LogEventProcessor.processPayload(logEventPayload);
      getClient(okapiHeaders, vertxContext).save(LOGS_TABLE_NAME, logRecord,
          reply -> {
            if (reply.failed()) {
              LOGGER.error("Error saving log record", reply.cause());
            }
          });
    } catch (IOException e) {
      LOGGER.error("Error saving log record", e);
    } finally {
      asyncResultHandler.handle(succeededFuture(PostAuditHandlersLogRecordResponse.respond204()));
    }
  }

}
