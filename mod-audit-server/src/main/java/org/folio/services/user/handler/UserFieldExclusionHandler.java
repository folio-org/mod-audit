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
import org.folio.services.user.UserFieldExclusionFilter;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserFieldExclusionHandler implements SettingChangeHandler {

  private static final Logger LOGGER = LogManager.getLogger();

  private final UserEventDao userEventDao;

  @Override
  public boolean isResponsible(String groupId, String settingKey) {
    return SettingGroup.USER.getId().equals(groupId) && SettingKey.EXCLUDED_FIELDS.getValue().equals(settingKey);
  }

  @Override
  public Future<Void> onSettingChanged(Object newValue, Conn conn, String tenantId) {
    var excludedPaths = UserFieldExclusionFilter.parseExcludedFields(newValue);
    if (excludedPaths.isEmpty()) {
      LOGGER.debug("onSettingChanged:: No excluded fields to apply [tenantId: {}]", tenantId);
      return Future.succeededFuture();
    }
    LOGGER.info("onSettingChanged:: Excluded fields updated, applying to existing records [excludedPaths: {}, tenantId: {}]",
      excludedPaths, tenantId);
    return userEventDao.excludeFieldsFromAll(excludedPaths, conn, tenantId)
      .compose(v -> userEventDao.deleteEmptyUpdateRecords(conn, tenantId));
  }
}
