package org.folio.services.user.handler;

import io.vertx.core.Future;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;
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
public class UserRetentionCleanupHandler implements SettingChangeHandler {

  private static final Logger LOGGER = LogManager.getLogger();

  private final UserEventDao userEventDao;

  @Override
  public boolean isResponsible(String groupId, String settingKey) {
    return SettingGroup.USER.getId().equals(groupId)
        && SettingKey.RETENTION_PERIOD.getValue().equals(settingKey);
  }

  @Override
  public Future<Void> onSettingChanged(Object newValue, Conn conn, String tenantId) {
    if (!(newValue instanceof Integer retentionPeriod)) {
      LOGGER.warn("onSettingChanged:: Unexpected value type for retention period setting [type: {}, tenantId: {}]",
        newValue == null ? "null" : newValue.getClass().getSimpleName(), tenantId);
      return Future.succeededFuture();
    }
    if (retentionPeriod <= 0) {
      LOGGER.debug("onSettingChanged:: Retention period is non-positive, skipping cleanup [retentionPeriod: {}, tenantId: {}]",
        retentionPeriod, tenantId);
      return Future.succeededFuture();
    }
    var expireOlderThan = new Timestamp(
      System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionPeriod));
    LOGGER.info("onSettingChanged:: Retention period set to {} days, deleting user audit records older than {} [tenantId: {}]",
      retentionPeriod, expireOlderThan, tenantId);
    return userEventDao.deleteOlderThanDate(expireOlderThan, conn, tenantId);
  }
}
