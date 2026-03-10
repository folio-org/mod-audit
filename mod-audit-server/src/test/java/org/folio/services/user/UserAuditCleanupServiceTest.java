package org.folio.services.user;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import org.folio.dao.user.UserEventDao;
import org.folio.rest.persist.Conn;
import org.folio.services.configuration.SettingGroup;
import org.folio.services.configuration.SettingKey;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class UserAuditCleanupServiceTest {

  private static final String USER_GROUP_ID = SettingGroup.USER.getId();
  private static final String ENABLED_KEY = SettingKey.ENABLED.getValue();
  private static final String PAGE_SIZE_KEY = SettingKey.RECORDS_PAGE_SIZE.getValue();

  @Mock
  private UserEventDao userEventDao;
  @Mock
  private Conn conn;
  @InjectMocks
  private UserAuditCleanupService cleanupService;

  @Test
  void onSettingChanged_shouldDeleteAll_whenEnabledChangedFromTrueToFalse() {
    when(userEventDao.deleteAll(conn, TENANT_ID)).thenReturn(Future.succeededFuture());

    var result = cleanupService.onSettingChanged(USER_GROUP_ID, ENABLED_KEY, true, false, conn, TENANT_ID);

    assertTrue(result.succeeded());
    verify(userEventDao).deleteAll(conn, TENANT_ID);
  }

  @Test
  void onSettingChanged_shouldNotDelete_whenEnabledChangedFromFalseToTrue() {
    var result = cleanupService.onSettingChanged(USER_GROUP_ID, ENABLED_KEY, false, true, conn, TENANT_ID);

    assertTrue(result.succeeded());
    verify(userEventDao, never()).deleteAll(conn, TENANT_ID);
  }

  @Test
  void onSettingChanged_shouldNotDelete_whenEnabledRemainsEnabled() {
    var result = cleanupService.onSettingChanged(USER_GROUP_ID, ENABLED_KEY, true, true, conn, TENANT_ID);

    assertTrue(result.succeeded());
    verify(userEventDao, never()).deleteAll(conn, TENANT_ID);
  }

  @Test
  void onSettingChanged_shouldNotDelete_whenEnabledRemainsDisabled() {
    var result = cleanupService.onSettingChanged(USER_GROUP_ID, ENABLED_KEY, false, false, conn, TENANT_ID);

    assertTrue(result.succeeded());
    verify(userEventDao, never()).deleteAll(conn, TENANT_ID);
  }

  @Test
  void onSettingChanged_shouldIgnoreNonEnabledSettingKey() {
    var result = cleanupService.onSettingChanged(USER_GROUP_ID, PAGE_SIZE_KEY, 10, 20, conn, TENANT_ID);

    assertTrue(result.succeeded());
    verify(userEventDao, never()).deleteAll(conn, TENANT_ID);
  }

  @Test
  void onSettingChanged_shouldIgnoreNonUserGroup() {
    var result = cleanupService.onSettingChanged(SettingGroup.INVENTORY.getId(), ENABLED_KEY, true, false, conn, TENANT_ID);

    assertTrue(result.succeeded());
    verify(userEventDao, never()).deleteAll(conn, TENANT_ID);
  }
}
