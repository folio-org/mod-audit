package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.HttpStatus.HTTP_BAD_REQUEST;
import static org.folio.util.ErrorUtils.buildError;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.rest.jaxrs.model.LogRecordCollection;
import org.folio.rest.jaxrs.resource.AuditDataCirculation;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.cql.CQLWrapper;

import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
            asyncResultHandler.handle(succeededFuture(GetAuditDataCirculationLogsResponse
              .respond200WithApplicationJson(new LogRecordCollection()
                .withLogRecords(reply.result().getResults())
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

  private CompletableFuture<CQLWrapper> createCqlWrapper(String tableName, String query, int limit, int offset) {
    CompletableFuture<CQLWrapper> future = new CompletableFuture<>();
    try {
      CQL2PgJSON cql2PgJSON = new CQL2PgJSON(tableName + ".jsonb");
      future.complete(new CQLWrapper(cql2PgJSON, query).setLimit(new Limit(limit)).setOffset(new Offset(offset)));
    } catch (FieldException e) {
      future.completeExceptionally(e);
    }
    return future;
  }
}
