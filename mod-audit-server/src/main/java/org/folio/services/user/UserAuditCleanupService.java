package org.folio.services.user;

import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.user.UserEventDao;
import org.folio.rest.persist.Conn;
import org.folio.services.configuration.SettingChangeHandler;
import org.folio.services.configuration.SettingGroup;
import org.folio.services.configuration.SettingKey;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAuditCleanupService implements SettingChangeHandler {

  private static final Logger LOGGER = LogManager.getLogger();

  private final UserEventDao userEventDao;

  @Override
  public Future<Void> onSettingChanged(String groupId, String settingKey,
                                       Object oldValue, Object newValue, Conn conn, String tenantId) {
    if (!SettingGroup.USER.getId().equals(groupId) || !SettingKey.ENABLED.getValue().equals(settingKey)) {
      return Future.succeededFuture();
    }
    return handleEnabledChange(oldValue, newValue, conn, tenantId);
  }

  private Future<Void> handleEnabledChange(Object oldValue, Object newValue, Conn conn, String tenantId) {
    if (Boolean.TRUE.equals(oldValue) && Boolean.FALSE.equals(newValue)) {
      LOGGER.info("handleEnabledChange:: User audit disabled, deleting all user audit records for [tenantId: {}]",
        tenantId);
      return userEventDao.deleteAll(conn, tenantId);
    }
    return Future.succeededFuture();
  }
}
