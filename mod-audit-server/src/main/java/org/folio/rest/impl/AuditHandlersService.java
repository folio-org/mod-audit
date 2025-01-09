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

public class AuditHandlersService extends BaseService implements AuditHandlers {
  private static final String SEARCH_BY_LOAN_ID_QUERY_PATTERN = "items=@loanId %s";

  private static final Logger LOGGER = LogManager.getLogger();

  @Override
  @Validate
  public void postAuditHandlersLogRecord(String entity, Map<String, String> okapiHeaders,
                                         Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    LOGGER.debug("postAuditHandlersLogRecord:: Trying to Save AuditHandlersLogRecord request with entity: {}", entity);
    try {
      JsonObject payload = new JsonObject(entity);
      LogRecordBuilder builder = LogRecordBuilderResolver.getBuilder(payload.getString(LOG_EVENT_TYPE.value()), okapiHeaders, vertxContext);
      builder.buildLogRecord(payload)
        .thenCompose(logRecords -> processAnonymize(logRecords, okapiHeaders, vertxContext))
        .thenCompose(logRecords -> saveLogRecords(logRecords, okapiHeaders, vertxContext))
        .exceptionally(throwable -> {
          LOGGER.warn("Error saving log event : {} due to : {}", entity, throwable.getLocalizedMessage());
          return null;
        });
    } catch (Exception e) {
      LOGGER.warn("Error saving log event for entity {} due to {} ", entity, e.getMessage());
    } finally {
      asyncResultHandler.handle(succeededFuture(PostAuditHandlersLogRecordResponse.respond204()));
    }
  }


  private CompletableFuture<List<LogRecord>> processAnonymize(List<LogRecord> records,
    Map<String, String> okapiHeaders, Context vertxContext) {
    LOGGER.debug("processAnonymize:: Processing anonymize for records");
    return isAnonymize(records) ?
      anonymizeLoanRelatedRecords(records, okapiHeaders, vertxContext) :
      CompletableFuture.completedFuture(records);
  }

  private boolean isAnonymize(List<LogRecord> records) {
    LOGGER.debug("isAnonymize:: Checking if anonymize is required for records");
    return records.stream()
      .anyMatch(logRecord -> LogRecord.Action.ANONYMIZE == logRecord.getAction());
  }

  private CompletableFuture<List<LogRecord>> anonymizeLoanRelatedRecords(List<LogRecord> records,
    Map<String, String> okapiHeaders, Context vertxContext) {
    LOGGER.debug("anonymizeLoanRelatedRecords:: Anonymize loan-related records for log records");
    CompletableFuture<List<LogRecord>> future = new CompletableFuture<>();
    List<LogRecord> result = new ArrayList<>();
    if (!records.isEmpty() && !records.get(0).getItems().isEmpty()) {
      result.add(records.get(0));
      createCqlWrapper(LOGS_TABLE_NAME, String.format(SEARCH_BY_LOAN_ID_QUERY_PATTERN, records.get(0).getItems().get(0).getLoanId()), Integer.MAX_VALUE, 0)
        .thenAccept(cqlWrapper -> getClient(okapiHeaders, vertxContext)
          .get(LOGS_TABLE_NAME, LogRecord.class, new String[] { "*" }, cqlWrapper, false, false, reply -> {
            if (reply.succeeded()) {
              LOGGER.info("anonymizeLoanRelatedRecords:: Anonymize loan-related records for log records Successfully");
              reply.result().getResults().forEach(logRecord -> {
                logRecord.setUserBarcode(null);
                logRecord.setLinkToIds(logRecord.getLinkToIds().withUserId(null));
                result.add(logRecord);
              });
              future.complete(result);
            } else {
              LOGGER.warn("Failed Anonymize loan-related records for log records due to : {}", reply.cause().getMessage());
              future.completeExceptionally(reply.cause());
            }
          }));
    }
    return future;
  }

  private CompletableFuture<Void> saveLogRecords(List<LogRecord> logRecords, Map<String, String> okapiHeaders,
    Context vertxContext) {
    LOGGER.debug("saveLogRecords:: Saving log records");
    CompletableFuture<Void> future = new CompletableFuture<>();
    getClient(okapiHeaders, vertxContext).upsertBatch(LOGS_TABLE_NAME, logRecords, reply -> {
      if (reply.failed()) {
        LOGGER.warn("Error saving log records: {}", reply.cause().getMessage());
        future.completeExceptionally(reply.cause());
      } else {
        future.complete(null);
      }
    });
    return future;
  }
}
