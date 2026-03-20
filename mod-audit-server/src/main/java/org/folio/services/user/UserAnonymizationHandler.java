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
public class UserAnonymizationHandler implements SettingChangeHandler {

  private static final Logger LOGGER = LogManager.getLogger();

  private final UserEventDao userEventDao;

  @Override
  public boolean isResponsible(String groupId, String settingKey) {
    return SettingGroup.USER.getId().equals(groupId) && SettingKey.ANONYMIZE.getValue().equals(settingKey);
  }

  @Override
  public Future<Void> onSettingChanged(Object newValue, Conn conn, String tenantId) {
    if (!(newValue instanceof Boolean)) {
      LOGGER.warn("onSettingChanged:: Unexpected value type for anonymize setting [type: {}, tenantId: {}]",
        newValue == null ? "null" : newValue.getClass().getSimpleName(), tenantId);
      return Future.succeededFuture();
    }
    if (Boolean.TRUE.equals(newValue)) {
      LOGGER.info("onSettingChanged:: Anonymization enabled, anonymizing all user audit records [tenantId: {}]",
        tenantId);
      return userEventDao.anonymizeAll(conn, tenantId)
        .compose(v -> userEventDao.deleteEmptyUpdateRecords(conn, tenantId));
    }
    return Future.succeededFuture();
  }
}
