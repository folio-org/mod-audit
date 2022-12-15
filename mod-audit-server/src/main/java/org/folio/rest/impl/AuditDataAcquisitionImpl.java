package org.folio.rest.impl;

import io.vertx.core.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.folio.rest.jaxrs.resource.AuditDataAcquisition;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.services.acquisition.OrderAuditEventsService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ExceptionHelper;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.Map;

import static java.lang.String.format;

public class AuditDataAcquisitionImpl implements AuditDataAcquisition {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private OrderAuditEventsService orderAuditEventsService;

  public AuditDataAcquisitionImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void getAuditDataAcquisitionOrderById(String id, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    String tenantId = TenantTool.tenantId(okapiHeaders);

    vertxContext.runOnContext(c -> {
      try {
        orderAuditEventsService.getAcquisitionOrderEventById(id, tenantId)
          .map(optionalOrderAuditEventDto -> optionalOrderAuditEventDto.orElseThrow(() ->
            new NotFoundException(format("OrderAuditEvent with id '%s' was not found", id))))
          .map(GetAuditDataAcquisitionOrderByIdResponse::respond200WithApplicationJson)
          .map(Response.class::cast)
          .otherwise(ExceptionHelper::mapExceptionToResponse)
          .onComplete(asyncResultHandler);
      } catch (Exception e) {
        LOGGER.error(getMessage("Failed to get OrderAuditEvent by id", e, id));
        asyncResultHandler.handle(Future.succeededFuture(ExceptionHelper.mapExceptionToResponse(e)));
      }
    });
  }
  private ParameterizedMessage getMessage(String pattern, Exception e, String... params) {
    return new ParameterizedMessage(pattern, new Object[] {params}, e);
  }
}
