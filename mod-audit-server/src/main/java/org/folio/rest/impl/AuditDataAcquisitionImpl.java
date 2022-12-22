package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.resource.AuditDataAcquisition;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.services.acquisition.OrderAuditEventsService;
import org.folio.services.acquisition.OrderLineAuditEventsService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ErrorUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;
import java.util.Map;

import static org.folio.util.ErrorCodes.GENERIC_ERROR_CODE;

public class AuditDataAcquisitionImpl implements AuditDataAcquisition {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private OrderAuditEventsService orderAuditEventsService;

  @Autowired
  private OrderLineAuditEventsService orderLineAuditEventsService;

  public AuditDataAcquisitionImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void getAuditDataAcquisitionOrderById(String orderId, String sortBy, String sortOrder, int limit, int offset, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    String tenantId = TenantTool.tenantId(okapiHeaders);

    vertxContext.runOnContext(c -> {
      try {
        orderAuditEventsService.getAuditEventsByOrderId(orderId, sortBy, sortOrder, limit, offset, tenantId)
          .map(GetAuditDataAcquisitionOrderByIdResponse::respond200WithApplicationJson)
          .map(Response.class::cast)
          .otherwise(this::mapExceptionToResponse)
          .onComplete(asyncResultHandler);
      } catch (Exception e) {
        LOGGER.error("Failed to get order audit events by order id: {}", orderId, e);
        asyncResultHandler.handle(Future.succeededFuture(mapExceptionToResponse(e)));
      }
    });
  }

  @Override
  public void getAuditDataAcquisitionOrderLineById(String orderLineId, String sortBy, String sortOrder, int limit, int offset, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    String tenantId = TenantTool.tenantId(okapiHeaders);

    vertxContext.runOnContext(c -> {
      try {
        orderLineAuditEventsService.getAuditEventsByOrderLineId(orderLineId, sortBy, sortOrder, limit, offset, tenantId)
          .map(GetAuditDataAcquisitionOrderLineByIdResponse::respond200WithApplicationJson)
          .map(Response.class::cast)
          .otherwise(this::mapExceptionToResponse)
          .onComplete(asyncResultHandler);
      } catch (Exception e) {
        LOGGER.error("Failed to get order line audit events by order line id: {}", orderLineId, e);
        asyncResultHandler.handle(Future.succeededFuture(mapExceptionToResponse(e)));
      }
    });
  }

  private Response mapExceptionToResponse(Throwable throwable) {
    LOGGER.error(throwable.getMessage(), throwable);
    return GetAuditDataAcquisitionOrderByIdResponse
         .respond500WithApplicationJson(ErrorUtils.buildErrors(GENERIC_ERROR_CODE.getCode(), throwable));
  }

}

