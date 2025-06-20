package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static java.util.Objects.isNull;
import static org.folio.HttpStatus.HTTP_BAD_REQUEST;
import static org.folio.util.Constants.NO_BARCODE;
import static org.folio.util.ErrorUtils.buildError;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.rest.jaxrs.model.LogRecordCollection;
import org.folio.rest.jaxrs.resource.AuditDataCirculation;
import org.folio.rest.persist.interfaces.Results;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class CirculationLogsService extends BaseService implements AuditDataCirculation {
  private static final Logger LOGGER = LogManager.getLogger();
  public static final String LOGS_TABLE_NAME = "circulation_logs";

  @Override
  @Validate
  public void getAuditDataCirculationLogs(String query, int offset, int limit, String lang,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    LOGGER.debug("getAuditDataCirculationLogs:: Getting audit data circulation logs");

    CompletableFuture<Results<LogRecord>> logRecordResultFuture = createCqlWrapper(LOGS_TABLE_NAME, query, limit, offset)
      .thenCompose(cqlWrapper -> getClient(okapiHeaders, vertxContext)
        .withReadTrans(conn -> conn.execute("SET LOCAL enable_indexscan = OFF;")
          .compose(rows -> conn.get(LOGS_TABLE_NAME, LogRecord.class, cqlWrapper, true))).toCompletionStage());

    logRecordResultFuture.thenAccept(result -> {
      LOGGER.info("getAuditDataCirculationLogs:: Successfully retrieved audit data circulation logs");

      List<LogRecord> logRecordList = result.getResults();
      logRecordList.stream().filter(logRecord -> isNull(logRecord.getUserBarcode()))
        .forEach(logRecord -> logRecord.setUserBarcode(NO_BARCODE));

      asyncResultHandler.handle(succeededFuture(GetAuditDataCirculationLogsResponse
        .respond200WithApplicationJson(new LogRecordCollection()
          .withLogRecords(logRecordList)
          .withTotalRecords(result.getResultInfo().getTotalRecords()))));
    }).exceptionally(throwable -> {
      LOGGER.warn("Exception occurred while getting audit data circulation logs: {}", throwable.getLocalizedMessage());
      asyncResultHandler.handle(succeededFuture(GetAuditDataCirculationLogsResponse.
        respond400WithApplicationJson(buildError(HTTP_BAD_REQUEST.toInt(), throwable.getLocalizedMessage()))));
      return null;
    });
  }
}
