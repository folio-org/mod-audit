package org.folio.rest.impl;

import static org.folio.util.ErrorCodes.GENERIC_ERROR_CODE;
import static org.folio.util.ErrorUtils.errorResponse;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.folio.rest.jaxrs.resource.AuditDataCleanup;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.services.management.AuditManager;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

public class AuditDataCleanupImpl implements AuditDataCleanup {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private AuditManager auditManager;

  public AuditDataCleanupImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void postAuditDataCleanupTimer(Map<String, String> okapiHeaders,
                                        Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    var tenantId = TenantTool.tenantId(okapiHeaders);
    LOGGER.debug("postAuditDataCleanupTimer:: Triggered for [tenantId: {}]", tenantId);
    auditManager.executeDatabaseCleanup(tenantId)
      .map(v -> AuditDataCleanup.PostAuditDataCleanupTimerResponse.respond204())
      .map(Response.class::cast)
      .otherwise(this::mapExceptionToResponse)
      .onComplete(asyncResultHandler);
  }

  private Response mapExceptionToResponse(Throwable throwable) {
    LOGGER.error("mapExceptionToResponse:: Error occurred during processing request", throwable);
    return errorResponse(HttpStatus.HTTP_INTERNAL_SERVER_ERROR, GENERIC_ERROR_CODE, throwable);
  }
}
