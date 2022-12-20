package org.folio.rest.impl;

import io.vertx.core.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.folio.rest.jaxrs.resource.AuditDataAcquisition;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.services.acquisition.OrderAuditEventsService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ErrorUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;
import java.util.Map;

import static org.folio.util.ErrorCodes.GENERIC_ERROR_CODE;

public class AuditDataAcquisitionImpl implements AuditDataAcquisition {

  private static final Logger LOGGER = LogManager.getLogger();

  private static final String TYPE = "application/json";

  @Autowired
  private OrderAuditEventsService orderAuditEventsService;

  public AuditDataAcquisitionImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void getAuditDataAcquisitionOrderById(String orderId, int limit, int offset,  Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    String tenantId = TenantTool.tenantId(okapiHeaders);

    vertxContext.runOnContext(c -> {
      try {
        orderAuditEventsService.getAuditEventsByOrderId(orderId, limit, offset, tenantId)
          .map(GetAuditDataAcquisitionOrderByIdResponse::respond200WithApplicationJson)
          .map(Response.class::cast)
          .otherwise(this::mapExceptionToResponse)
          .onComplete(asyncResultHandler);
      } catch (Exception e) {
        LOGGER.error(getMessage("Failed to get OrderAuditEvent by id", e, orderId));
        asyncResultHandler.handle(Future.succeededFuture(mapExceptionToResponse(e)));
      }
    });
  }

  private ParameterizedMessage getMessage(String pattern, Exception e, String... params) {
    return new ParameterizedMessage(pattern, new Object[] {params}, e);
  }

  private Response mapExceptionToResponse(Throwable throwable) {
    LOGGER.error(throwable.getMessage(), throwable);
    return AuditDataAcquisitionImpl.GetAuditDataAcquisitionOrderByIdResponse
         .respond500WithApplicationJson(ErrorUtils.buildErrors(GENERIC_ERROR_CODE.getCode(), throwable));
  }

}

