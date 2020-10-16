package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.impl.CirculationLogsService.LOGS_TABLE_NAME;
import static org.folio.util.LogEventPayloadField.LOG_EVENT_TYPE;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.builder.LogRecordBuilderResolver;
import org.folio.builder.service.LogRecordBuilderService;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.rest.jaxrs.resource.AuditHandlers;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class AuditHandlersService extends BaseService implements AuditHandlers {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuditHandlersService.class);

  @Override
  @Validate
  public void postAuditHandlersLogRecord(String entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
      JsonObject payload = new JsonObject(entity);
      LogRecordBuilderService builder = LogRecordBuilderResolver.getBuilder(payload.getString(LOG_EVENT_TYPE.value()));
      List<LogRecord> logRecord = builder.buildLogRecord(new JsonObject(entity));
      getClient(okapiHeaders, vertxContext).saveBatch(LOGS_TABLE_NAME, logRecord, reply -> {
        if (reply.failed()) {
          LOGGER.error("Error saving log event: " + entity, reply.cause());
        } else {
          asyncResultHandler.handle(succeededFuture(PostAuditHandlersLogRecordResponse.respond204()));
        }
      });
    } catch (Exception e) {
      LOGGER.error("Error saving log event: " + entity);
    } finally {
      asyncResultHandler.handle(succeededFuture(PostAuditHandlersLogRecordResponse.respond204()));
    }
  }
}
