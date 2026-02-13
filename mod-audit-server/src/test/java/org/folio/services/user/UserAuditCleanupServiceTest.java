package org.folio.services.user;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import org.folio.dao.user.UserEventDao;
import org.folio.services.configuration.SettingGroup;
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

  @Mock
  private UserEventDao userEventDao;
  @InjectMocks
  private UserAuditCleanupService cleanupService;

  @Test
  void onSettingChanged_shouldDeleteAll_whenEnabledChangedFromTrueToFalse() {
    when(userEventDao.deleteAll(TENANT_ID)).thenReturn(Future.succeededFuture());

    var result = cleanupService.onSettingChanged(USER_GROUP_ID, "enabled", true, false, TENANT_ID);

    assertTrue(result.succeeded());
    verify(userEventDao).deleteAll(TENANT_ID);
  }

  @Test
  void onSettingChanged_shouldNotDelete_whenEnabledChangedFromFalseToTrue() {
    var result = cleanupService.onSettingChanged(USER_GROUP_ID, "enabled", false, true, TENANT_ID);

    assertTrue(result.succeeded());
    verify(userEventDao, never()).deleteAll(TENANT_ID);
  }

  @Test
  void onSettingChanged_shouldNotDelete_whenEnabledUnchanged() {
    var result = cleanupService.onSettingChanged(USER_GROUP_ID, "enabled", true, true, TENANT_ID);

    assertTrue(result.succeeded());
    verify(userEventDao, never()).deleteAll(TENANT_ID);
  }

  @Test
  void onSettingChanged_shouldIgnoreNonEnabledSettingKey() {
    var result = cleanupService.onSettingChanged(USER_GROUP_ID, "records.page.size", 10, 20, TENANT_ID);

    assertTrue(result.succeeded());
    verify(userEventDao, never()).deleteAll(TENANT_ID);
  }

  @Test
  void onSettingChanged_shouldIgnoreNonUserGroup() {
    var result = cleanupService.onSettingChanged("audit.inventory", "enabled", true, false, TENANT_ID);

    assertTrue(result.succeeded());
    verify(userEventDao, never()).deleteAll(TENANT_ID);
  }
}
