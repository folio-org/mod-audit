package org.folio.services.user;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import java.sql.Timestamp;
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
class UserRetentionCleanupHandlerTest {

  private static final String USER_GROUP_ID = SettingGroup.USER.getId();
  private static final String RETENTION_PERIOD_KEY = SettingKey.RETENTION_PERIOD.getValue();
  private static final String ENABLED_KEY = SettingKey.ENABLED.getValue();

  @Mock
  private UserEventDao userEventDao;
  @Mock
  private Conn conn;
  @InjectMocks
  private UserRetentionCleanupHandler cleanupHandler;

  @Test
  void onSettingChanged_shouldDeleteExpired_whenPositiveRetention() {
    when(userEventDao.deleteOlderThanDate(any(Timestamp.class), eq(conn), eq(TENANT_ID)))
      .thenReturn(Future.succeededFuture());

    var result = cleanupHandler.onSettingChanged(10, conn, TENANT_ID);

    assertTrue(result.succeeded());
    verify(userEventDao).deleteOlderThanDate(any(Timestamp.class), eq(conn), eq(TENANT_ID));
  }

  @Test
  void onSettingChanged_shouldNotDelete_whenValueIsNull() {
    var result = cleanupHandler.onSettingChanged(null, conn, TENANT_ID);

    assertTrue(result.succeeded());
    verify(userEventDao, never()).deleteOlderThanDate(any(Timestamp.class), any(Conn.class), any());
  }

  @Test
  void onSettingChanged_shouldNotDelete_whenValueIsNotInteger() {
    var result = cleanupHandler.onSettingChanged("not-an-integer", conn, TENANT_ID);

    assertTrue(result.succeeded());
    verify(userEventDao, never()).deleteOlderThanDate(any(Timestamp.class), any(Conn.class), any());
  }

  @Test
  void onSettingChanged_shouldNotDelete_whenNegativeRetention() {
    var result = cleanupHandler.onSettingChanged(-1, conn, TENANT_ID);

    assertTrue(result.succeeded());
    verify(userEventDao, never()).deleteOlderThanDate(any(Timestamp.class), any(Conn.class), any());
  }

  @Test
  void onSettingChanged_shouldNotDelete_whenZeroRetention() {
    var result = cleanupHandler.onSettingChanged(0, conn, TENANT_ID);

    assertTrue(result.succeeded());
    verify(userEventDao, never()).deleteOlderThanDate(any(Timestamp.class), any(Conn.class), any());
  }

  @ParameterizedTest
  @MethodSource("isResponsibleCases")
  void isResponsible_shouldMatchOnlyUserRetentionPeriodSetting(String groupId, String settingKey, boolean expected) {
    assertEquals(expected, cleanupHandler.isResponsible(groupId, settingKey));
  }

  static Stream<Arguments> isResponsibleCases() {
    return Stream.of(
      Arguments.of(USER_GROUP_ID, RETENTION_PERIOD_KEY, true),
      Arguments.of(USER_GROUP_ID, ENABLED_KEY, false),
      Arguments.of(SettingGroup.INVENTORY.getId(), RETENTION_PERIOD_KEY, false)
    );
  }
}
