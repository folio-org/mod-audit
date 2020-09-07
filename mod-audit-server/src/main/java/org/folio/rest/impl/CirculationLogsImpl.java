package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.HttpStatus.HTTP_BAD_REQUEST;
import static org.folio.HttpStatus.HTTP_UNPROCESSABLE_ENTITY;
import static org.folio.util.ErrorUtils.buildError;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.rest.RestVerticle;
import org.folio.rest.annotations.Validate;
import org.folio.rest.handler.LogEventProcessor;
import org.folio.rest.handler.LogObject;
import org.folio.rest.jaxrs.model.LogEventPayload;
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
  @Override
  @Validate
  public void getAuditDataCirculationLogs(String query, String lang, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    List<CompletableFuture<List<LogRecord>>> futures = Arrays.stream(LogObject.values())
      .map(LogObject::tableName)
      .map(tableName -> getLogsFromTable(tableName, query, okapiHeaders, vertxContext))
      .collect(Collectors.toList());

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
      .thenApply(vVoid -> futures.stream()
        .map(CompletableFuture::join)
        .flatMap(Collection::stream)
        .collect(Collectors.toList()))
      .thenApply(list -> new LogRecordCollection().withLogRecords(list).withTotalRecords(list.size()))
      .thenAccept(logRecordCollection -> asyncResultHandler
        .handle(succeededFuture(GetAuditDataCirculationLogsResponse
          .respond200WithApplicationJson(logRecordCollection))))
      .exceptionally(throwable -> {
        asyncResultHandler.handle(succeededFuture(GetAuditDataCirculationLogsResponse.
          respond400WithApplicationJson(buildError(HTTP_BAD_REQUEST.toInt(), throwable.getLocalizedMessage()))));
        return null;
      });
  }

  @Override
  public void postAuditDataCirculationEventHandler(String entity, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    LogRecord logRecord = LogEventProcessor.processPayload(new JsonObject(entity).mapTo(LogEventPayload.class));
    getClient(okapiHeaders, vertxContext).save(LogObject.fromName(logRecord.getObject().value()).tableName(), logRecord, reply -> {
      if (reply.succeeded()) {
        asyncResultHandler.handle(succeededFuture(PostAuditDataCirculationEventHandlerResponse
          .respond201()));
      } else {
        asyncResultHandler.handle(succeededFuture(PostAuditDataCirculationEventHandlerResponse
          .respond422WithApplicationJson(buildError(HTTP_UNPROCESSABLE_ENTITY.toInt(), reply.cause().getLocalizedMessage()))));
      }
    });
  }

  private CompletableFuture<List<LogRecord>> getLogsFromTable(String tableName, String query,
    Map<String, String> okapiHeaders, Context vertxContext) {
    CompletableFuture<List<LogRecord>> future = new CompletableFuture<>();
    createCqlWrapper(tableName, query)
      .thenAccept(cqlWrapper -> getClient(okapiHeaders, vertxContext)
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

  private PostgresClient getClient(Map<String, String> okapiHeaders, Context vertxContext) {
    String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
    return PostgresClient.getInstance(vertxContext.owner(), tenantId);
  }
}
