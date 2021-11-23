package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.impl.CirculationLogsService.LOGS_TABLE_NAME;
import static org.folio.util.LogEventPayloadField.LOG_EVENT_TYPE;

import java.util.ArrayList;
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
  private static final String SEARCH_BY_LOAN_ID_QUERY_PATTERN = "items=@loanId %s";

  private static final Logger LOGGER = LogManager.getLogger();

  @Override
  @Validate
  public void postAuditHandlersLogRecord(String entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
      JsonObject payload = new JsonObject(entity);
      LogRecordBuilder builder = LogRecordBuilderResolver.getBuilder(payload.getString(LOG_EVENT_TYPE.value()), okapiHeaders, vertxContext);
      builder.buildLogRecord(payload)
        .thenCompose(logRecords -> processAnonymize(logRecords, okapiHeaders, vertxContext))
        .thenCompose(logRecords -> saveLogRecords(logRecords, okapiHeaders, vertxContext))
        .exceptionally(throwable -> {
          LOGGER.error("Error saving log event: " + entity, throwable);
          return null;
        });
    } catch (Exception e) {
      LOGGER.error("Error saving log event: " + entity, e);
    } finally {
      asyncResultHandler.handle(succeededFuture(PostAuditHandlersLogRecordResponse.respond204()));
    }
  }

  private CompletableFuture<List<LogRecord>> processAnonymize(List<LogRecord> records,
    Map<String, String> okapiHeaders, Context vertxContext) {
    return isAnonymize(records) ?
      anonymizeLoanRelatedRecords(records, okapiHeaders, vertxContext) :
      CompletableFuture.completedFuture(records);
  }

  private boolean isAnonymize(List<LogRecord> records) {
    return records.stream()
      .anyMatch(logRecord -> LogRecord.Action.ANONYMIZE == logRecord.getAction());
  }

  private CompletableFuture<List<LogRecord>> anonymizeLoanRelatedRecords(List<LogRecord> records,
    Map<String, String> okapiHeaders, Context vertxContext) {
    CompletableFuture<List<LogRecord>> future = new CompletableFuture<>();
    List<LogRecord> result = new ArrayList<>();
    if (!records.isEmpty() && !records.get(0).getItems().isEmpty()) {
      result.add(records.get(0));
      createCqlWrapper(LOGS_TABLE_NAME, String.format(SEARCH_BY_LOAN_ID_QUERY_PATTERN, records.get(0).getItems().get(0).getLoanId()), Integer.MAX_VALUE, 0)
        .thenAccept(cqlWrapper -> getClient(okapiHeaders, vertxContext)
          .get(LOGS_TABLE_NAME, LogRecord.class, new String[] { "*" }, cqlWrapper, false, false, reply -> {
            if (reply.succeeded()) {
              reply.result().getResults().forEach(record -> {
                record.setUserBarcode(null);
                record.setLinkToIds(record.getLinkToIds().withUserId(null));
                result.add(record);
              });
              future.complete(result);
            } else {
              future.completeExceptionally(reply.cause());
            }
          }));
    }
    return future;
  }

  private CompletableFuture<Void> saveLogRecords(List<LogRecord> logRecords, Map<String, String> okapiHeaders,
    Context vertxContext) {
    CompletableFuture<Void> future = new VertxCompletableFuture<>(vertxContext);
    getClient(okapiHeaders, vertxContext).upsertBatch(LOGS_TABLE_NAME, logRecords, reply -> {
      if (reply.failed()) {
        future.completeExceptionally(reply.cause());
      } else {
        future.complete(null);
      }
    });
    return future;
  }
}
