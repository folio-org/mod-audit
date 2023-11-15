package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.AuditDataAcquisitionOrderIdGetSortOrder;
import org.folio.rest.jaxrs.model.AuditDataAcquisitionOrderLineIdGetSortOrder;
import org.folio.rest.jaxrs.model.AuditDataAcquisitionPieceIdGetSortOrder;
import org.folio.rest.jaxrs.model.AuditDataAcquisitionPieceIdStatusChangeHistoryGetSortOrder;
import org.folio.rest.jaxrs.resource.AuditDataAcquisition;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.services.acquisition.OrderAuditEventsService;
import org.folio.services.acquisition.OrderLineAuditEventsService;
import org.folio.services.acquisition.PieceAuditEventsService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ErrorUtils;

import javax.ws.rs.core.Response;
import java.util.Map;

import static org.folio.util.ErrorCodes.GENERIC_ERROR_CODE;

public class AuditDataAcquisitionImpl implements AuditDataAcquisition {

  private static final Logger LOGGER = LogManager.getLogger();

  private final OrderAuditEventsService orderAuditEventsService;
  private final OrderLineAuditEventsService orderLineAuditEventsService;
  private final PieceAuditEventsService pieceAuditEventsService;

  public AuditDataAcquisitionImpl(OrderAuditEventsService orderAuditEventsService,
                                  OrderLineAuditEventsService orderLineAuditEventsService,
                                  PieceAuditEventsService pieceAuditEventsService) {
    this.orderAuditEventsService = orderAuditEventsService;
    this.orderLineAuditEventsService = orderLineAuditEventsService;
    this.pieceAuditEventsService = pieceAuditEventsService;
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void getAuditDataAcquisitionOrderById(String orderId, String sortBy, AuditDataAcquisitionOrderIdGetSortOrder sortOrder,
                                               int limit, int offset, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    LOGGER.debug("getAuditDataAcquisitionOrderById:: Retrieving Audit Data Acquisition Order By Id : {}", orderId);
    String tenantId = TenantTool.tenantId(okapiHeaders);

    try {
      LOGGER.warn("Trying to get audit events by order id: {}", orderId);
      orderAuditEventsService.getAuditEventsByOrderId(orderId, sortBy, sortOrder.name(), limit, offset, tenantId)
        .map(GetAuditDataAcquisitionOrderByIdResponse::respond200WithApplicationJson)
        .map(Response.class::cast)
        .otherwise(this::mapExceptionToResponse)
        .onComplete(asyncResultHandler);
    } catch (Exception e) {
      LOGGER.warn("Failed to get order audit events by order id: {}", orderId, e);
      asyncResultHandler.handle(Future.succeededFuture(mapExceptionToResponse(e)));
    }
  }

  @Override
  public void getAuditDataAcquisitionOrderLineById(String orderLineId, String sortBy, AuditDataAcquisitionOrderLineIdGetSortOrder sortOrder,
                                                   int limit, int offset, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    LOGGER.debug("getAuditDataAcquisitionOrderLineById:: Retrieving Audit Data Acquisition Order Line By Id : {}", orderLineId);
    String tenantId = TenantTool.tenantId(okapiHeaders);

    try {
      LOGGER.warn("Trying to get audit events by order line id: {}", orderLineId);
      orderLineAuditEventsService.getAuditEventsByOrderLineId(orderLineId, sortBy, sortOrder.name(), limit, offset, tenantId)
        .map(GetAuditDataAcquisitionOrderLineByIdResponse::respond200WithApplicationJson)
        .map(Response.class::cast)
        .otherwise(this::mapExceptionToResponse)
        .onComplete(asyncResultHandler);
    } catch (Exception e) {
      LOGGER.warn("Failed to get order line audit events by order line id: {}", orderLineId, e);
      asyncResultHandler.handle(Future.succeededFuture(mapExceptionToResponse(e)));
    }

  }

  @Override
  public void getAuditDataAcquisitionPieceById(String pieceId, String sortBy, AuditDataAcquisitionPieceIdGetSortOrder sortOrder,
                                               int limit, int offset, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    LOGGER.debug("getAuditDataAcquisitionOrderById:: Retrieving Audit Data Acquisition Piece By Id : {}", pieceId);
    String tenantId = TenantTool.tenantId(okapiHeaders);

    try {
      pieceAuditEventsService.getAuditEventsByPieceId(pieceId, sortBy, sortOrder.name(), limit, offset, tenantId)
        .map(GetAuditDataAcquisitionPieceByIdResponse::respond200WithApplicationJson)
        .map(Response.class::cast)
        .otherwise(this::mapExceptionToResponse)
        .onComplete(asyncResultHandler);
    } catch (Exception e) {
      LOGGER.error("Failed to get piece audit events by piece id: {}", pieceId, e);
      asyncResultHandler.handle(Future.succeededFuture(mapExceptionToResponse(e)));
    }
  }

  @Override
  public void getAuditDataAcquisitionPieceStatusChangeHistoryById(String pieceId, String sortBy,
                                                                  AuditDataAcquisitionPieceIdStatusChangeHistoryGetSortOrder sortOrder,
                                                                  int limit, int offset, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    LOGGER.debug("getAuditDataAcquisitionOrderById:: Retrieving Audit Data Acquisition Piece with unique status By Id : {}", pieceId);
    String tenantId = TenantTool.tenantId(okapiHeaders);

    try {
      LOGGER.warn("Trying to get piece audit events with unique status by piece id: {}", pieceId);
      pieceAuditEventsService.getAuditEventsWithStatusChangesByPieceId(pieceId, sortBy, sortOrder.name(), limit, offset, tenantId)
        .map(GetAuditDataAcquisitionPieceByIdResponse::respond200WithApplicationJson)
        .map(Response.class::cast)
        .otherwise(this::mapExceptionToResponse)
        .onComplete(asyncResultHandler);
    } catch (Exception e) {
      LOGGER.warn("Failed to get piece audit events with unique status change by piece id: {}", pieceId, e);
      asyncResultHandler.handle(Future.succeededFuture(mapExceptionToResponse(e)));
    }
  }

  private Response mapExceptionToResponse(Throwable throwable) {
    LOGGER.debug("mapExceptionToResponse:: Mapping Exception :{} to Response", throwable.getMessage(), throwable);
    return GetAuditDataAcquisitionOrderByIdResponse
      .respond500WithApplicationJson(ErrorUtils.buildErrors(GENERIC_ERROR_CODE.getCode(), throwable));
  }
}

