package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static java.util.Objects.isNull;
import static org.folio.HttpStatus.HTTP_BAD_REQUEST;
import static org.folio.util.ErrorUtils.buildError;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.rest.jaxrs.model.LogRecordCollection;
import org.folio.rest.jaxrs.resource.AuditDataCirculation;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class CirculationLogsService extends BaseService implements AuditDataCirculation {
  public static final String LOGS_TABLE_NAME = "circulation_logs";

  @Override
  @Validate
  public void getAuditDataCirculationLogs(String query, int offset, int limit, String lang,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    createCqlWrapper(LOGS_TABLE_NAME, query, limit, offset)
      .thenAccept(cqlWrapper -> getClient(okapiHeaders, vertxContext)
        .get(LOGS_TABLE_NAME, LogRecord.class, new String[] { "*" }, cqlWrapper, true, false, reply -> {
          if (reply.succeeded()) {
            var results = reply.result().getResults();
            results.stream().filter(logRecord -> isNull(logRecord.getUserBarcode()))
              .forEach(logRecord -> logRecord.setUserBarcode("-"));
            asyncResultHandler.handle(succeededFuture(GetAuditDataCirculationLogsResponse
              .respond200WithApplicationJson(new LogRecordCollection()
                .withLogRecords(results)
                .withTotalRecords(reply.result().getResultInfo().getTotalRecords()))));
          } else {
            asyncResultHandler.handle(succeededFuture(GetAuditDataCirculationLogsResponse
              .respond400WithApplicationJson(buildError(HTTP_BAD_REQUEST.toInt(), reply.cause().getLocalizedMessage()))));
          }
        }))
      .exceptionally(throwable -> {
        asyncResultHandler.handle(succeededFuture(GetAuditDataCirculationLogsResponse.
          respond400WithApplicationJson(buildError(HTTP_BAD_REQUEST.toInt(), throwable.getLocalizedMessage()))));
        return null;
      });
  }
}
