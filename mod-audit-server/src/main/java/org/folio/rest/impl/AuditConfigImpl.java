package org.folio.rest.impl;

import static org.folio.util.ErrorCodes.GENERIC_ERROR_CODE;
import static org.folio.util.ErrorCodes.NOT_FOUND_ERROR_CODE;
import static org.folio.util.ErrorCodes.UNAUTHORIZED_ERROR_CODE;
import static org.folio.util.ErrorCodes.VALIDATION_ERROR_CODE;
import static org.folio.util.ErrorUtils.errorResponse;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.Map;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.folio.exception.UnauthorizedOperationException;
import org.folio.exception.ValidationException;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.jaxrs.model.Setting;
import org.folio.rest.jaxrs.resource.AuditConfig;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.services.configuration.ConfigurationService;
import org.folio.services.configuration.PermissionCheckerHelper;
import org.folio.spring.SpringContextUtil;
import org.folio.util.ErrorUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class AuditConfigImpl implements AuditConfig {

  private static final Logger LOGGER = LogManager.getLogger();

  private static final String GET_GROUP_SETTING_PERMISSION = "audit.config.groups.settings.%s.collection.get";
  private static final String PUT_GROUP_SETTING_PERMISSION = "audit.config.groups.settings.%s.%s.item.put";

  @Autowired
  private ConfigurationService configurationService;

  public AuditConfigImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void getAuditConfigGroups(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
                                   Context vertxContext) {
    LOGGER.debug("getAuditConfigGroups:: Getting all setting groups");
    String tenantId = TenantTool.tenantId(okapiHeaders);
    try {
      configurationService.getAllSettingGroups(tenantId)
        .map(GetAuditConfigGroupsResponse::respond200WithApplicationJson)
        .map(Response.class::cast)
        .otherwise(this::mapExceptionToResponse)
        .onComplete(asyncResultHandler);
    } catch (Exception e) {
      LOGGER.error("getAuditConfigGroups:: Error getting all setting groups", e);
      asyncResultHandler.handle(Future.succeededFuture(mapExceptionToResponse(e)));
    }
  }

  @Override
  public void getAuditConfigGroupsSettingsByGroupId(String groupId, Map<String, String> okapiHeaders,
                                                    Handler<AsyncResult<Response>> asyncResultHandler,
                                                    Context vertxContext) {
    LOGGER.debug("getAuditConfigGroupsSettingsByGroupId:: Getting all settings by groupId: {}", groupId);
    String tenantId = TenantTool.tenantId(okapiHeaders);
    var desiredPermission = GET_GROUP_SETTING_PERMISSION.formatted(groupId);
    try {
      PermissionCheckerHelper.checkPermission(okapiHeaders, desiredPermission)
        .compose(aVoid -> configurationService.getAllSettingsByGroupId(groupId, tenantId))
        .map(GetAuditConfigGroupsSettingsByGroupIdResponse::respond200WithApplicationJson)
        .map(Response.class::cast)
        .otherwise(this::mapExceptionToResponse)
        .onComplete(asyncResultHandler);
    } catch (Exception e) {
      LOGGER.error("getAuditConfigGroupsSettingsByGroupId:: Error getting all settings by groupId: {}", groupId, e);
      asyncResultHandler.handle(Future.succeededFuture(mapExceptionToResponse(e)));
    }
  }

  @Override
  public void putAuditConfigGroupsSettingsByGroupIdAndSettingId(String groupId, String settingId, Setting entity,
                                                                Map<String, String> okapiHeaders,
                                                                Handler<AsyncResult<Response>> asyncResultHandler,
                                                                Context vertxContext) {
    LOGGER.debug("putAuditConfigGroupsSettingsByGroupIdAndSettingId:: "
                 + "Updating setting by groupId: {} and settingId: {}", groupId, settingId);
    String tenantId = TenantTool.tenantId(okapiHeaders);
    var userId = okapiHeaders.get(XOkapiHeaders.USER_ID);
    var desiredPermission = PUT_GROUP_SETTING_PERMISSION.formatted(groupId, settingId);
    try {
      PermissionCheckerHelper.checkPermission(okapiHeaders, desiredPermission)
        .compose(aVoid -> configurationService.updateSetting(groupId, settingId, entity, userId, tenantId))
        .map(aVoid -> Response.noContent().build())
        .otherwise(this::mapExceptionToResponse)
        .onComplete(asyncResultHandler);
    } catch (Exception e) {
      LOGGER.error("putAuditConfigGroupsSettingsByGroupIdAndSettingId:: "
                   + "Error updating setting by groupId: {} and settingId: {}", groupId, settingId, e);
      asyncResultHandler.handle(Future.succeededFuture(mapExceptionToResponse(e)));
    }
  }

  private Response mapExceptionToResponse(Throwable throwable) {
    if (throwable instanceof ValidationException) {
      return errorResponse(HttpStatus.HTTP_UNPROCESSABLE_ENTITY, VALIDATION_ERROR_CODE, throwable);
    } else if (throwable instanceof NotFoundException) {
      return errorResponse(HttpStatus.HTTP_NOT_FOUND, NOT_FOUND_ERROR_CODE, throwable);
    } else if (throwable instanceof UnauthorizedOperationException) {
      return errorResponse(HttpStatus.HTTP_FORBIDDEN, UNAUTHORIZED_ERROR_CODE, throwable);
    }
    LOGGER.error("mapExceptionToResponse:: Mapping Exception :{} to Response", throwable.getMessage(), throwable);
    return GetAuditConfigGroupsResponse
      .respond400WithApplicationJson(ErrorUtils.buildErrors(GENERIC_ERROR_CODE.getCode(), throwable));
  }
}
