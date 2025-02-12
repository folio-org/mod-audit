package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.resource.AuditDataMarc;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.services.marc.MarcAuditService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ErrorUtils;
import org.folio.util.marc.SourceRecordType;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;
import java.util.Map;

import static org.folio.util.ErrorCodes.GENERIC_ERROR_CODE;

public class MarcBibAuditImpl implements AuditDataMarc {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private MarcAuditService service;

  public MarcBibAuditImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }


  @Override
  public void getAuditDataMarcBibByEntityId(String entityId, int limit, int offset,
                                            Map<String, String> okapiHeaders,
                                            Handler<AsyncResult<Response>> asyncResultHandler,
                                            Context vertxContext) {
    LOGGER.debug("getAuditDataMarcBibByEntityId:: Retrieving Marc Bib Audit Data by entityId: '{}'", entityId);
    var tenantId = TenantTool.tenantId(okapiHeaders);
    try {
      service.getMarcAuditRecords(entityId, SourceRecordType.MARC_BIB, tenantId, limit, offset)
        .map(AuditDataMarc.GetAuditDataMarcBibByEntityIdResponse::respond200WithApplicationJson)
        .map(Response.class::cast)
        .otherwise(this::mapExceptionToResponse)
        .onComplete(asyncResultHandler);
    } catch (Exception e) {
      LOGGER.error("getAuditDataMarcBibByEntityId:: Error retrieving Marc Bib Audit Data by entityId: '{}'", entityId, e);
      asyncResultHandler.handle(Future.succeededFuture(mapExceptionToResponse(e)));
    }
  }

  private Response mapExceptionToResponse(Throwable throwable) {
    LOGGER.debug("mapExceptionToResponse:: Mapping Exception :{} to Response", throwable.getMessage(), throwable);
    return AuditDataMarc.GetAuditDataMarcBibByEntityIdResponse
      .respond500WithApplicationJson(ErrorUtils.buildErrors(GENERIC_ERROR_CODE.getCode(), throwable));
  }
}
