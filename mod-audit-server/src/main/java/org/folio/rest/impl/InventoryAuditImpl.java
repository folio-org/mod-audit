package org.folio.rest.impl;

import static org.folio.util.ErrorCodes.GENERIC_ERROR_CODE;
import static org.folio.util.ErrorCodes.VALIDATION_ERROR_CODE;
import static org.folio.util.ErrorUtils.errorResponse;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.folio.exception.ValidationException;
import org.folio.rest.jaxrs.resource.AuditDataInventory;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.services.inventory.InventoryEventService;
import org.folio.spring.SpringContextUtil;
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
  public void getAuditDataInventoryInstanceByEntityId(String entityId, String eventTs, Map<String, String> okapiHeaders,
                                                      Handler<AsyncResult<Response>> asyncResultHandler,
                                                      Context vertxContext) {
    LOGGER.debug(
      "getAuditDataInventoryInstanceByEntityId:: Retrieving Audit Data Inventory Instance by [entityId: {}, eventTs: {}]",
      entityId, eventTs);
    var tenantId = TenantTool.tenantId(okapiHeaders);
    try {
      service.getEvents(InventoryResourceType.INSTANCE, entityId, eventTs, tenantId)
        .map(AuditDataInventory.GetAuditDataInventoryInstanceByEntityIdResponse::respond200WithApplicationJson)
        .map(Response.class::cast)
        .otherwise(this::mapExceptionToResponse)
        .onComplete(asyncResultHandler);
    } catch (Exception e) {
      LOGGER.error(
        "getAuditDataInventoryInstanceByEntityId:: Error retrieving Audit Data Inventory Instance by [entityId: {}, eventDate: {}]",
        entityId, eventTs, e);
      asyncResultHandler.handle(Future.succeededFuture(mapExceptionToResponse(e)));
    }
  }

  @Override
  public void getAuditDataInventoryHoldingsByEntityId(String entityId, String eventTs, Map<String, String> okapiHeaders,
                                                      Handler<AsyncResult<Response>> asyncResultHandler,
                                                      Context vertxContext) {
    LOGGER.debug(
      "getAuditDataInventoryHoldingsByEntityId:: Retrieving Audit Data Inventory Holdings by [entityId: {}, eventTs: {}]",
      entityId, eventTs);
    var tenantId = TenantTool.tenantId(okapiHeaders);
    try {
      service.getEvents(InventoryResourceType.HOLDINGS, entityId, eventTs, tenantId)
        .map(AuditDataInventory.GetAuditDataInventoryHoldingsByEntityIdResponse::respond200WithApplicationJson)
        .map(Response.class::cast)
        .otherwise(this::mapExceptionToResponse)
        .onComplete(asyncResultHandler);
    } catch (Exception e) {
      LOGGER.error(
        "getAuditDataInventoryHoldingsByEntityId:: Error retrieving Audit Data Inventory Holdings by [entityId: {}, eventDate: {}]",
        entityId, eventTs, e);
      asyncResultHandler.handle(Future.succeededFuture(mapExceptionToResponse(e)));
    }
  }

  @Override
  public void getAuditDataInventoryItemByEntityId(String entityId, String eventTs, Map<String, String> okapiHeaders,
                                                  Handler<AsyncResult<Response>> asyncResultHandler,
                                                  Context vertxContext) {
    LOGGER.debug(
      "getAuditDataInventoryItemByEntityId:: Retrieving Audit Data Inventory Item by [entityId: {}, eventTs: {}]",
      entityId, eventTs);
    var tenantId = TenantTool.tenantId(okapiHeaders);
    try {
      service.getEvents(InventoryResourceType.ITEM, entityId, eventTs, tenantId)
        .map(AuditDataInventory.GetAuditDataInventoryItemByEntityIdResponse::respond200WithApplicationJson)
        .map(Response.class::cast)
        .otherwise(this::mapExceptionToResponse)
        .onComplete(asyncResultHandler);
    } catch (Exception e) {
      LOGGER.error(
        "getAuditDataInventoryItemByEntityId:: Error retrieving Audit Data Inventory Item by [entityId: {}, eventDate: {}]",
        entityId, eventTs, e);
      asyncResultHandler.handle(Future.succeededFuture(mapExceptionToResponse(e)));
    }
  }

  private Response mapExceptionToResponse(Throwable throwable) {
    LOGGER.debug("mapExceptionToResponse:: Mapping Exception :{} to Response", throwable.getMessage(), throwable);
    if (throwable instanceof ValidationException) {
      return errorResponse(HttpStatus.HTTP_BAD_REQUEST, VALIDATION_ERROR_CODE, throwable);
    }
    LOGGER.error("mapExceptionToResponse:: Error occurred during processing request", throwable);
    return errorResponse(HttpStatus.HTTP_INTERNAL_SERVER_ERROR, GENERIC_ERROR_CODE, throwable);
  }
}
