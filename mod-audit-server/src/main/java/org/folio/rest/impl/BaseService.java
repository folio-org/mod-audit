package org.folio.rest.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.rest.RestVerticle;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.Context;

import static org.folio.rest.impl.CirculationLogsService.OPTIMISED_TRUE;

public abstract class BaseService {

  PostgresClient getClient(Map<String, String> okapiHeaders, Context vertxContext) {
    String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
    return PostgresClient.getInstance(vertxContext.owner(), tenantId);
  }

  CompletableFuture<CQLWrapper> createCqlWrapper(String tableName, String query, int limit, int offset) {
    CompletableFuture<CQLWrapper> future = new CompletableFuture<>();
    String tempQuery = query;
    try {
      CQL2PgJSON cql2PgJSON = new CQL2PgJSON(tableName + ".jsonb");
      if(query!=null &&  query.contains(OPTIMISED_TRUE)){
        System.out.println("replacing with empty");
        tempQuery = query.replace(OPTIMISED_TRUE,"");
      }
        future.complete(new CQLWrapper(cql2PgJSON, tempQuery).setLimit(new Limit(limit)).setOffset(new Offset(offset)));

    } catch (FieldException e) {
      future.completeExceptionally(e);
    }
    return future;
  }
}
