package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.HttpStatus.HTTP_BAD_REQUEST;
import static org.folio.util.Constants.DB_TAB_FEES_FINES;
import static org.folio.util.Constants.DB_TAB_ITEM_BLOCKS;
import static org.folio.util.Constants.DB_TAB_LOANS;
import static org.folio.util.Constants.DB_TAB_MANUAL_BLOCKS;
import static org.folio.util.Constants.DB_TAB_NOTICES;
import static org.folio.util.Constants.DB_TAB_PATRON_BLOCKS;
import static org.folio.util.Constants.DB_TAB_REQUESTS;
import static org.folio.util.ErrorUtils.buildError;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.rest.RestVerticle;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.rest.jaxrs.model.LogRecordCollection;
import org.folio.rest.jaxrs.resource.AuditDataCirculation;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.tools.utils.TenantTool;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CirculationLogsImpl implements AuditDataCirculation {
  private final List<String> tableNames = Arrays.asList(DB_TAB_FEES_FINES, DB_TAB_ITEM_BLOCKS, DB_TAB_LOANS,
    DB_TAB_MANUAL_BLOCKS, DB_TAB_NOTICES, DB_TAB_PATRON_BLOCKS, DB_TAB_REQUESTS);

  @Override
  @Validate
  public void getAuditDataCirculationLogs(String query, String lang, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    List<CompletableFuture<List<LogRecord>>> futures = tableNames.stream()
      .map(tableName -> getLogsFromTable(tableName, query, okapiHeaders, vertxContext))
      .collect(Collectors.toList());

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
      .thenApply(vVoid -> futures.stream()
        .map(CompletableFuture::join)
        .flatMap(Collection::stream)
        .collect(Collectors.toList()))
      .thenApply(list -> new LogRecordCollection().withLogRecords(list).withTotalRecords(list.size()))
      .thenAccept(logRecordCollection -> asyncResultHandler
        .handle(succeededFuture(AuditDataCirculation.GetAuditDataCirculationLogsResponse
          .respond200WithApplicationJson(logRecordCollection))))
      .exceptionally(throwable -> {
        asyncResultHandler.handle(succeededFuture(AuditDataCirculation.GetAuditDataCirculationLogsResponse.
          respond400WithApplicationJson(buildError(HTTP_BAD_REQUEST.toInt(), throwable.getLocalizedMessage()))));
        return null;
      });
  }

  private CompletableFuture<List<LogRecord>> getLogsFromTable(String tableName, String query,
    Map<String, String> okapiHeaders, Context vertxContext) {
    CompletableFuture<List<LogRecord>> future = new CompletableFuture<>();
    String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
    createCqlWrapper(tableName, query)
      .thenAccept(cqlWrapper -> PostgresClient.getInstance(vertxContext.owner(), tenantId)
        .get(tableName, LogRecord.class, new String[] { "*" }, cqlWrapper, false, false, reply -> {
          if (reply.succeeded()) {
            future.complete(reply.result().getResults());
          } else {
            future.completeExceptionally(reply.cause());
          }
        }))
      .exceptionally(throwable -> {
        future.completeExceptionally(throwable);
        return null;
      });
    return future;
  }

  private CompletableFuture<CQLWrapper> createCqlWrapper(String tableName, String query) {
    CompletableFuture<CQLWrapper> future = new CompletableFuture<>();
    try {
      CQL2PgJSON cql2PgJSON = new CQL2PgJSON(tableName + ".jsonb");
      future.complete(new CQLWrapper(cql2PgJSON, query));
    } catch (FieldException e) {
      future.completeExceptionally(e);
    }
    return future;
  }
}
