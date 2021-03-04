package org.folio.rest.impl;

import java.util.Map;

import org.folio.rest.RestVerticle;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.Context;

public abstract class BaseService {

  PostgresClient getClient(Map<String, String> okapiHeaders, Context vertxContext) {
    String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
    return PostgresClient.getInstance(vertxContext.owner(), tenantId);
  }

}
