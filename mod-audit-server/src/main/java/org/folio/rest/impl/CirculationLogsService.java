package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static java.util.Objects.isNull;
import static org.folio.HttpStatus.HTTP_BAD_REQUEST;
import static org.folio.util.Constants.NO_BARCODE;
import static org.folio.util.ErrorUtils.buildError;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.rest.jaxrs.model.LogRecordCollection;
import org.folio.rest.jaxrs.resource.AuditDataCirculation;

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
    createCqlWrapper(LOGS_TABLE_NAME, query, limit, offset)
      .thenAccept(cqlWrapper -> getClient(okapiHeaders, vertxContext)
        .get(LOGS_TABLE_NAME, LogRecord.class, new String[] { "*" }, cqlWrapper, true, false, reply -> {
          if (reply.succeeded()) {
            LOGGER.info("getAuditDataCirculationLogs:: Successfully retrieved audit data circulation logs");
            var results = reply.result().getResults();
            results.stream().filter(logRecord -> isNull(logRecord.getUserBarcode()))
              .forEach(logRecord -> logRecord.setUserBarcode(NO_BARCODE));
            asyncResultHandler.handle(succeededFuture(GetAuditDataCirculationLogsResponse
              .respond200WithApplicationJson(new LogRecordCollection()
                .withLogRecords(results)
                .withTotalRecords(reply.result().getResultInfo().getTotalRecords()))));
          } else {
            LOGGER.warn("Failed to retrieve audit data circulation logs: {}", reply.cause().getMessage());
            asyncResultHandler.handle(succeededFuture(GetAuditDataCirculationLogsResponse
              .respond400WithApplicationJson(buildError(HTTP_BAD_REQUEST.toInt(), reply.cause().getLocalizedMessage()))));
          }
        }))
      .exceptionally(throwable -> {
        LOGGER.warn("Exception occurred while getting audit data circulation logs: {}", throwable.getMessage());
        asyncResultHandler.handle(succeededFuture(GetAuditDataCirculationLogsResponse.
          respond400WithApplicationJson(buildError(HTTP_BAD_REQUEST.toInt(), throwable.getLocalizedMessage()))));
        return null;
      });
  }
}
