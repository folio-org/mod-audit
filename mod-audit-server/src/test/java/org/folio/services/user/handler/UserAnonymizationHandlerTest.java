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
class UserAnonymizationHandlerTest {

  private static final String USER_GROUP_ID = SettingGroup.USER.getId();
  private static final String ANONYMIZE_KEY = SettingKey.ANONYMIZE.getValue();
  private static final String ENABLED_KEY = SettingKey.ENABLED.getValue();

  @Mock
  private UserEventDao userEventDao;
  @Mock
  private Conn conn;
  @InjectMocks
  private UserAnonymizationHandler anonymizationHandler;

  @Test
  void onSettingChanged_shouldAnonymizeAll_whenSetToTrue() {
    when(userEventDao.anonymizeAll(conn, TENANT_ID)).thenReturn(Future.succeededFuture());
    when(userEventDao.deleteEmptyUpdateRecords(conn, TENANT_ID)).thenReturn(Future.succeededFuture());

    var result = anonymizationHandler.onSettingChanged(true, conn, TENANT_ID);

    assertTrue(result.succeeded());
    verify(userEventDao).anonymizeAll(conn, TENANT_ID);
    verify(userEventDao).deleteEmptyUpdateRecords(conn, TENANT_ID);
  }

  @Test
  void onSettingChanged_shouldNotAnonymize_whenSetToFalse() {
    var result = anonymizationHandler.onSettingChanged(false, conn, TENANT_ID);

    assertTrue(result.succeeded());
    verify(userEventDao, never()).anonymizeAll(conn, TENANT_ID);
  }

  @Test
  void onSettingChanged_shouldNotAnonymize_whenValueIsNull() {
    var result = anonymizationHandler.onSettingChanged(null, conn, TENANT_ID);

    assertTrue(result.succeeded());
    verify(userEventDao, never()).anonymizeAll(conn, TENANT_ID);
  }

  @Test
  void onSettingChanged_shouldNotAnonymize_whenValueIsUnexpectedType() {
    var result = anonymizationHandler.onSettingChanged("unexpected", conn, TENANT_ID);

    assertTrue(result.succeeded());
    verify(userEventDao, never()).anonymizeAll(conn, TENANT_ID);
  }

  @ParameterizedTest
  @MethodSource("isResponsibleCases")
  void isResponsible_shouldMatchOnlyUserAnonymizeSetting(String groupId, String settingKey, boolean expected) {
    assertEquals(expected, anonymizationHandler.isResponsible(groupId, settingKey));
  }

  static Stream<Arguments> isResponsibleCases() {
    return Stream.of(
      Arguments.of(USER_GROUP_ID, ANONYMIZE_KEY, true),
      Arguments.of(USER_GROUP_ID, ENABLED_KEY, false),
      Arguments.of(SettingGroup.INVENTORY.getId(), ANONYMIZE_KEY, false)
    );
  }
}
