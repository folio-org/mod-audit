package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.folio.exception.ValidationException;
import org.folio.rest.jaxrs.resource.AuditDataMarc;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.services.marc.MarcAuditService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.marc.SourceRecordType;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;
import java.util.Map;

import static org.folio.util.ErrorCodes.GENERIC_ERROR_CODE;
import static org.folio.util.ErrorCodes.VALIDATION_ERROR_CODE;
import static org.folio.util.ErrorUtils.errorResponse;

public class MarcAuditImpl implements AuditDataMarc {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  MarcAuditService service;

  public MarcAuditImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void getAuditDataMarcBibByEntityId(String entityId, String eventTs, Map<String, String> okapiHeaders,
                                            Handler<AsyncResult<Response>> asyncResultHandler,
                                            Context vertxContext) {
    LOGGER.debug("getAuditDataMarcBibByEntityId:: Retrieving Marc Bib Audit Data by entityId: '{}'", entityId);
    var tenantId = TenantTool.tenantId(okapiHeaders);
    try {
      service.getMarcAuditRecords(entityId, SourceRecordType.MARC_BIB, tenantId, eventTs)
        .map(AuditDataMarc.GetAuditDataMarcBibByEntityIdResponse::respond200WithApplicationJson)
        .map(Response.class::cast)
        .otherwise(this::mapExceptionToResponse)
        .onComplete(asyncResultHandler);
    } catch (Exception e) {
      LOGGER.error("getAuditDataMarcBibByEntityId:: Error retrieving Marc Bib Audit Data by entityId: '{}'", entityId, e);
      asyncResultHandler.handle(Future.succeededFuture(mapExceptionToResponse(e)));
    }
  }

  @Override
  public void getAuditDataMarcAuthorityByEntityId(String entityId, String eventTs, Map<String, String> okapiHeaders,
                                                  Handler<AsyncResult<Response>> asyncResultHandler,
                                                  Context vertxContext) {
    LOGGER.debug("getAuditDataMarcAuthorityByEntityId:: Retrieving Authority Audit Data by entityId: '{}'", entityId);
    var tenantId = TenantTool.tenantId(okapiHeaders);
    try {
      service.getMarcAuditRecords(entityId, SourceRecordType.MARC_AUTHORITY, tenantId, eventTs)
        .map(AuditDataMarc.GetAuditDataMarcAuthorityByEntityIdResponse::respond200WithApplicationJson)
        .map(Response.class::cast)
        .otherwise(this::mapExceptionToResponse)
        .onComplete(asyncResultHandler);
    } catch (Exception e) {
      LOGGER.error("getAuditDataMarcAuthorityByEntityId:: Error retrieving Marc Authority Audit Data by entityId: '{}'", entityId, e);
      asyncResultHandler.handle(Future.succeededFuture(mapExceptionToResponse(e)));
    }
  }

  private Response mapExceptionToResponse(Throwable throwable) {
    LOGGER.debug("mapExceptionToResponse:: Mapping Exception :{} to Response", throwable.getMessage(), throwable);
    if (throwable instanceof ValidationException) {
      return errorResponse(HttpStatus.HTTP_BAD_REQUEST, VALIDATION_ERROR_CODE, throwable);
    }
    return errorResponse(HttpStatus.HTTP_INTERNAL_SERVER_ERROR, GENERIC_ERROR_CODE, throwable);
  }
}
