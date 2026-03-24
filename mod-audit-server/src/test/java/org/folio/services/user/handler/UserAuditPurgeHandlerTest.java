package org.folio.services.user.handler;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import java.util.stream.Stream;
import org.folio.dao.user.UserEventDao;
import org.folio.rest.persist.Conn;
import org.folio.services.configuration.SettingGroup;
import org.folio.services.configuration.SettingKey;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class UserAuditPurgeHandlerTest {

  private static final String USER_GROUP_ID = SettingGroup.USER.getId();
  private static final String ENABLED_KEY = SettingKey.ENABLED.getValue();
  private static final String PAGE_SIZE_KEY = SettingKey.RECORDS_PAGE_SIZE.getValue();

  @Mock
  private UserEventDao userEventDao;
  @Mock
  private Conn conn;
  @InjectMocks
  private UserAuditPurgeHandler purgeHandler;

  @Test
  void onSettingChanged_shouldDeleteAll_whenSetToFalse() {
    when(userEventDao.deleteAll(conn, TENANT_ID)).thenReturn(Future.succeededFuture());

    var result = purgeHandler.onSettingChanged(false, conn, TENANT_ID);

    assertTrue(result.succeeded());
    verify(userEventDao).deleteAll(conn, TENANT_ID);
  }

  @Test
  void onSettingChanged_shouldNotDelete_whenSetToTrue() {
    var result = purgeHandler.onSettingChanged(true, conn, TENANT_ID);

    assertTrue(result.succeeded());
    verify(userEventDao, never()).deleteAll(conn, TENANT_ID);
  }

  @ParameterizedTest
  @MethodSource("isResponsibleCases")
  void isResponsible_shouldMatchOnlyUserEnabledSetting(String groupId, String settingKey, boolean expected) {
    assertEquals(expected, purgeHandler.isResponsible(groupId, settingKey));
  }

  static Stream<Arguments> isResponsibleCases() {
    return Stream.of(
      Arguments.of(USER_GROUP_ID, ENABLED_KEY, true),
      Arguments.of(USER_GROUP_ID, PAGE_SIZE_KEY, false),
      Arguments.of(SettingGroup.INVENTORY.getId(), ENABLED_KEY, false)
    );
  }
}
