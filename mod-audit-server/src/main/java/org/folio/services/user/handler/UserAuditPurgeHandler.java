package org.folio.services.user.handler;

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
public class UserAuditPurgeHandler implements SettingChangeHandler {

  private static final Logger LOGGER = LogManager.getLogger();

  private final UserEventDao userEventDao;

  @Override
  public boolean isResponsible(String groupId, String settingKey) {
    return SettingGroup.USER.getId().equals(groupId) && SettingKey.ENABLED.getValue().equals(settingKey);
  }

  @Override
  public Future<Void> onSettingChanged(Object newValue, Conn conn, String tenantId) {
    if (Boolean.FALSE.equals(newValue)) {
      LOGGER.info("onSettingChanged:: User audit disabled, deleting all user audit records [tenantId: {}]", tenantId);
      return userEventDao.deleteAll(conn, tenantId);
    }
    return Future.succeededFuture();
  }
}
