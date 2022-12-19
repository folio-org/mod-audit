package org.folio.rest.impl;

import io.vertx.core.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.folio.rest.jaxrs.resource.AuditDataAcquisition;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.services.acquisition.OrderAuditEventsService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.Map;

import static java.lang.String.format;

public class AuditDataAcquisitionImpl implements AuditDataAcquisition {

  private static final Logger LOGGER = LogManager.getLogger();

  private static final String TYPE = "text/plain";

  @Autowired
  private OrderAuditEventsService orderAuditEventsService;

  public AuditDataAcquisitionImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void getAuditDataAcquisitionOrderById(String OrderId, int limit, int offset,  Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    String tenantId = TenantTool.tenantId(okapiHeaders);

    vertxContext.runOnContext(c -> {
      try {
        orderAuditEventsService.getAuditEventsByOrderId(OrderId, limit, offset, tenantId)
          .map(optionalOrderAuditEvent -> optionalOrderAuditEvent)
          .map(GetAuditDataAcquisitionOrderByIdResponse::respond200WithApplicationJson)
          .map(Response.class::cast)
          .otherwise(e -> mapExceptionToResponse(e))
          .onComplete(asyncResultHandler);
      } catch (Exception e) {
        LOGGER.error(getMessage("Failed to get OrderAuditEvent by id", e, OrderId));
        asyncResultHandler.handle(Future.succeededFuture(mapExceptionToResponse(e)));
      }
    });
  }

  private ParameterizedMessage getMessage(String pattern, Exception e, String... params) {
    return new ParameterizedMessage(pattern, new Object[] {params}, e);
  }

  private Response mapExceptionToResponse(Throwable throwable) {
     if (throwable instanceof NotFoundException) {
       LOGGER.error(throwable.getMessage(), throwable);
       return Response.status(Response.Status.NOT_FOUND.getStatusCode()).type(TYPE).entity(throwable.getMessage()).build();
     }
     else {
        LOGGER.error(throwable.getMessage(), throwable);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).type("text/plain").entity(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase()).build();
     }
  }
}

