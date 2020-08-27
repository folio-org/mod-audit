package org.folio.rest.impl;

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
import org.folio.rest.tools.utils.ValidationHelper;

import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.vertx.core.Future.succeededFuture;

public class CirculationLogsImpl implements AuditDataCirculation {
  protected static final String DB_TAB_FEES_FINES = "fees_fines";
  protected static final String DB_TAB_ITEM_BLOCKS = "item_blocks";
  protected static final String DB_TAB_LOANS = "loans";
  protected static final String DB_TAB_MANUAL_BLOCKS = "manual_blocks";
  protected static final String DB_TAB_NOTICES = "notices";
  protected static final String DB_TAB_PATRON_BLOCKS = "patron_blocks";
  protected static final String DB_TAB_REQUESTS = "requests";

  @Override
  @Validate
  public void getAuditDataCirculationLogs(String query, String lang, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    List<CompletableFuture<List<LogRecord>>> futures = Stream.of(DB_TAB_FEES_FINES, DB_TAB_ITEM_BLOCKS,
      DB_TAB_LOANS, DB_TAB_MANUAL_BLOCKS, DB_TAB_NOTICES, DB_TAB_PATRON_BLOCKS, DB_TAB_REQUESTS)
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
        ValidationHelper.handleError(throwable, asyncResultHandler);
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
