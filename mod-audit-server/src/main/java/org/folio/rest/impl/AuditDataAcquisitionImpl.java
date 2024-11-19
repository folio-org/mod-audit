package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.AuditDataAcquisitionInvoiceIdGetSortOrder;
import org.folio.rest.jaxrs.model.AuditDataAcquisitionInvoiceLineIdGetSortOrder;
import org.folio.rest.jaxrs.model.AuditDataAcquisitionOrderIdGetSortOrder;
import org.folio.rest.jaxrs.model.AuditDataAcquisitionOrderLineIdGetSortOrder;
import org.folio.rest.jaxrs.model.AuditDataAcquisitionOrganizationIdGetSortOrder;
import org.folio.rest.jaxrs.model.AuditDataAcquisitionPieceIdGetSortOrder;
import org.folio.rest.jaxrs.model.AuditDataAcquisitionPieceIdStatusChangeHistoryGetSortOrder;
import org.folio.rest.jaxrs.resource.AuditDataAcquisition;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.services.acquisition.InvoiceAuditEventsService;
import org.folio.services.acquisition.InvoiceLineAuditEventsService;
import org.folio.services.acquisition.OrderAuditEventsService;
import org.folio.services.acquisition.OrderLineAuditEventsService;
import org.folio.services.acquisition.OrganizationAuditEventsService;
import org.folio.services.acquisition.PieceAuditEventsService;
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
  @Autowired
  private PieceAuditEventsService pieceAuditEventsService;
  @Autowired
  private InvoiceAuditEventsService invoiceAuditEventsService;
  @Autowired
  private InvoiceLineAuditEventsService invoiceLineAuditEventsService;
  @Autowired
  private OrganizationAuditEventsService organizationAuditEventsService;

  public AuditDataAcquisitionImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void getAuditDataAcquisitionOrderById(String orderId, String sortBy, AuditDataAcquisitionOrderIdGetSortOrder sortOrder,
                                               int limit, int offset, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    LOGGER.debug("getAuditDataAcquisitionOrderById:: Retrieving Audit Data Acquisition Order By Id : {}", orderId);
    String tenantId = TenantTool.tenantId(okapiHeaders);
    try {
      orderAuditEventsService.getAuditEventsByOrderId(orderId, sortBy, sortOrder.name(), limit, offset, tenantId)
        .map(GetAuditDataAcquisitionOrderByIdResponse::respond200WithApplicationJson)
        .map(Response.class::cast)
        .otherwise(this::mapExceptionToResponse)
        .onComplete(asyncResultHandler);
    } catch (Exception e) {
      LOGGER.error("Failed to get order audit events by order id: {}", orderId, e);
      asyncResultHandler.handle(Future.succeededFuture(mapExceptionToResponse(e)));
    }
  }

  @Override
  public void getAuditDataAcquisitionOrderLineById(String orderLineId, String sortBy, AuditDataAcquisitionOrderLineIdGetSortOrder sortOrder,
                                                   int limit, int offset, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    LOGGER.debug("getAuditDataAcquisitionOrderLineById:: Retrieving Audit Data Acquisition Order Line By Id : {}", orderLineId);
    String tenantId = TenantTool.tenantId(okapiHeaders);
    try {
      orderLineAuditEventsService.getAuditEventsByOrderLineId(orderLineId, sortBy, sortOrder.name(), limit, offset, tenantId)
        .map(GetAuditDataAcquisitionOrderLineByIdResponse::respond200WithApplicationJson)
        .map(Response.class::cast)
        .otherwise(this::mapExceptionToResponse)
        .onComplete(asyncResultHandler);
    } catch (Exception e) {
      LOGGER.error("Failed to get order line audit events by order line id: {}", orderLineId, e);
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
    LOGGER.debug("getAuditDataAcquisitionOrderById:: Retrieving Audit Data Acquisition Piece with status changes By Id : {}", pieceId);
    String tenantId = TenantTool.tenantId(okapiHeaders);
    try {
      pieceAuditEventsService.getAuditEventsWithStatusChangesByPieceId(pieceId, sortBy, sortOrder.name(), limit, offset, tenantId)
        .map(GetAuditDataAcquisitionPieceByIdResponse::respond200WithApplicationJson)
        .map(Response.class::cast)
        .otherwise(this::mapExceptionToResponse)
        .onComplete(asyncResultHandler);
    } catch (Exception e) {
      LOGGER.error("Failed to get piece audit events with unique status change by piece id: {}", pieceId, e);
      asyncResultHandler.handle(Future.succeededFuture(mapExceptionToResponse(e)));
    }
  }

  @Override
  public void getAuditDataAcquisitionInvoiceById(String invoiceId, String sortBy,
                                                 AuditDataAcquisitionInvoiceIdGetSortOrder sortOrder,
                                                 int limit, int offset, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    LOGGER.debug("getAuditDataAcquisitionOrderLineById:: Retrieving Audit Data Acquisition Invoice By Id : {}", invoiceId);
    String tenantId = TenantTool.tenantId(okapiHeaders);
    try {
      invoiceAuditEventsService.getAuditEventsByInvoiceId(invoiceId, sortBy, sortOrder.name(), limit, offset, tenantId)
        .map(GetAuditDataAcquisitionInvoiceByIdResponse::respond200WithApplicationJson)
        .map(Response.class::cast)
        .otherwise(this::mapExceptionToResponse)
        .onComplete(asyncResultHandler);
    } catch (Exception e) {
      LOGGER.error("Failed to get invoice audit events by invoice id: {}", invoiceId, e);
      asyncResultHandler.handle(Future.succeededFuture(mapExceptionToResponse(e)));
    }
  }

  @Override
  public void getAuditDataAcquisitionInvoiceLineById(String invoiceLineId, String sortBy, AuditDataAcquisitionInvoiceLineIdGetSortOrder sortOrder,
                                                     int limit, int offset, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    LOGGER.debug("getAuditDataAcquisitionInvoiceLineById:: Retrieving Audit Data Acquisition Invoice Line By Id : {}", invoiceLineId);
    String tenantId = TenantTool.tenantId(okapiHeaders);
    try {
      invoiceLineAuditEventsService.getAuditEventsByInvoiceLineId(invoiceLineId, sortBy, sortOrder.name(), limit, offset, tenantId)
        .map(GetAuditDataAcquisitionInvoiceLineByIdResponse::respond200WithApplicationJson)
        .map(Response.class::cast)
        .otherwise(this::mapExceptionToResponse)
        .onComplete(asyncResultHandler);
    } catch (Exception e) {
      LOGGER.error("Failed to get invoice line audit events by invoice line id: {}", invoiceLineId, e);
      asyncResultHandler.handle(Future.succeededFuture(mapExceptionToResponse(e)));
    }
  }

  @Override
  public void getAuditDataAcquisitionOrganizationById(String organizationId, String sortBy,
                                                      AuditDataAcquisitionOrganizationIdGetSortOrder sortOrder,
                                                      int limit, int offset, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    LOGGER.debug("getAuditDataAcquisitionOrganizationById:: Retrieving Audit Data Acquisition Organization By Id : {}", organizationId);
    String tenantId = TenantTool.tenantId(okapiHeaders);
    try {
      organizationAuditEventsService.getAuditEventsByOrganizationId(organizationId, sortBy, sortOrder.name(), limit, offset, tenantId)
        .map(GetAuditDataAcquisitionOrganizationByIdResponse::respond200WithApplicationJson)
        .map(Response.class::cast)
        .otherwise(this::mapExceptionToResponse)
        .onComplete(asyncResultHandler);
    } catch (Exception e) {
      LOGGER.error("Failed to get organization audit events by organization id: {}", organizationId, e);
      asyncResultHandler.handle(Future.succeededFuture(mapExceptionToResponse(e)));
    }
  }

  private Response mapExceptionToResponse(Throwable throwable) {
    LOGGER.debug("mapExceptionToResponse:: Mapping Exception :{} to Response", throwable.getMessage(), throwable);
    return GetAuditDataAcquisitionOrderByIdResponse
      .respond500WithApplicationJson(ErrorUtils.buildErrors(GENERIC_ERROR_CODE.getCode(), throwable));
  }
}

