package org.folio.rest.impl;

import static org.folio.util.ErrorCodes.GENERIC_ERROR_CODE;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.resource.AuditDataInventory;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.services.inventory.InventoryEventService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ErrorUtils;
import org.folio.util.inventory.InventoryResourceType;
import org.springframework.beans.factory.annotation.Autowired;

public class InventoryAuditImpl implements AuditDataInventory {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private InventoryEventService service;

  public InventoryAuditImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void getAuditDataInventoryInstanceByEntityId(String entityId, String eventDate,
                                                      Map<String, String> okapiHeaders,
                                                      Handler<AsyncResult<Response>> asyncResultHandler,
                                                      Context vertxContext) {
    LOGGER.debug(
      "getAuditDataInventoryInstanceByEntityId:: Retrieving Audit Data Inventory Instance by [entityId: {}, eventDate: {}]",
      entityId, eventDate);
    var tenantId = TenantTool.tenantId(okapiHeaders);
    try {
      service.getEvents(InventoryResourceType.INSTANCE, entityId, eventDate, tenantId)
        .map(AuditDataInventory.GetAuditDataInventoryInstanceByEntityIdResponse::respond200WithApplicationJson)
        .map(Response.class::cast)
        .otherwise(this::mapExceptionToResponse)
        .onComplete(asyncResultHandler);
    } catch (Exception e) {
      LOGGER.error(
        "getAuditDataInventoryInstanceByEntityId:: Error retrieving Audit Data Inventory Instance by [entityId: {}, eventDate: {}]",
        entityId, eventDate, e);
      asyncResultHandler.handle(Future.succeededFuture(mapExceptionToResponse(e)));
    }
  }


  private Response mapExceptionToResponse(Throwable throwable) {
    LOGGER.debug("mapExceptionToResponse:: Mapping Exception :{} to Response", throwable.getMessage(), throwable);
    return AuditDataInventory.GetAuditDataInventoryInstanceByEntityIdResponse
      .respond500WithApplicationJson(ErrorUtils.buildErrors(GENERIC_ERROR_CODE.getCode(), throwable));
  }
}
