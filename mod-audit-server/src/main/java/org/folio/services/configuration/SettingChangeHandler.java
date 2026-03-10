package org.folio.services.configuration;

import io.vertx.core.Future;
import org.folio.rest.persist.Conn;

/**
 * Handler invoked when a configuration setting value changes.
 *
 * <p>Implementations are called within the active write transaction managed by
 * {@link ConfigurationService#updateSetting}. The provided {@code conn} may be used for
 * transactional DB operations that must be atomic with the setting update itself.
 *
 * <p>Implementations must not commit or roll back {@code conn}.
 * Non-DB side effects (HTTP calls, Kafka messages, etc.) should not be performed via this callback.
 */
@FunctionalInterface
public interface SettingChangeHandler {

  Future<Void> onSettingChanged(String groupId, String settingKey,
                                Object oldValue, Object newValue, Conn conn, String tenantId);
}
