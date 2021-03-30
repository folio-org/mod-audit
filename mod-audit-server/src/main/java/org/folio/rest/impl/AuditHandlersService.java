package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.impl.CirculationLogsService.LOGS_TABLE_NAME;
import static org.folio.util.LogEventPayloadField.LOG_EVENT_TYPE;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.builder.LogRecordBuilderResolver;
import org.folio.builder.service.LogRecordBuilder;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.rest.jaxrs.resource.AuditHandlers;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

public class AuditHandlersService extends BaseService implements AuditHandlers {

  private static final Logger LOGGER = LogManager.getLogger();

  @Override
  @Validate
  public void postAuditHandlersLogRecord(String entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
      JsonObject payload = new JsonObject(entity);
      LogRecordBuilder builder = LogRecordBuilderResolver.getBuilder(payload.getString(LOG_EVENT_TYPE.value()), okapiHeaders, vertxContext);
      builder.buildLogRecord(payload)
        .thenCompose(logRecords -> saveLogRecords(logRecords, okapiHeaders, vertxContext))
        .exceptionally(throwable -> {
          LOGGER.error("Error saving log event: " + entity, throwable.getLocalizedMessage());
          return null;
        });
    } catch (Exception e) {
      LOGGER.error("Error saving log event: " + entity);
    } finally {
      asyncResultHandler.handle(succeededFuture(PostAuditHandlersLogRecordResponse.respond204()));
    }
  }

  private CompletableFuture<Void> saveLogRecords(List<LogRecord> logRecords, Map<String, String> okapiHeaders,
    Context vertxContext) {
    CompletableFuture<Void> future = new VertxCompletableFuture<>(vertxContext);
    getClient(okapiHeaders, vertxContext).saveBatch(LOGS_TABLE_NAME, logRecords, reply -> {
      if (reply.failed()) {
        future.completeExceptionally(reply.cause());
      } else {
        future.complete(null);
      }
    });
    return future;
  }
}
