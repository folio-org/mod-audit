package org.folio.services.configuration;

import io.vertx.core.Future;

@FunctionalInterface
public interface SettingChangeHandler {

  Future<Void> onSettingChanged(String groupId, String settingKey,
                                Object oldValue, Object newValue, String tenantId);
}
