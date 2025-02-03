package org.folio.services.configuration;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import lombok.experimental.UtilityClass;
import org.folio.exception.UnauthorizedOperationException;
import org.folio.okapi.common.XOkapiHeaders;

import java.util.Map;

@UtilityClass
public class PermissionCheckerHelper {

  public static Future<Void> checkPermission(Map<String, String> okapiHeaders, String requiredPermission) {
    var okapiPermissions = new JsonArray(okapiHeaders.get(XOkapiHeaders.PERMISSIONS));
    if (!okapiPermissions.contains(requiredPermission)) {
      return Future.failedFuture(new UnauthorizedOperationException(requiredPermission));
    }
    return Future.succeededFuture();
  }
}