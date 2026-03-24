package org.folio.services.user.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import java.util.Set;
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
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class UserFieldExclusionHandlerTest {

  private static final String USER_GROUP_ID = SettingGroup.USER.getId();
  private static final String EXCLUDED_FIELDS_KEY = SettingKey.EXCLUDED_FIELDS.getValue();
  private static final String ENABLED_KEY = SettingKey.ENABLED.getValue();

  @Mock
  private UserEventDao userEventDao;
  @Mock
  private Conn conn;
  @InjectMocks
  private UserFieldExclusionHandler exclusionHandler;

  @Test
  void onSettingChanged_shouldExcludeFieldsAndDeleteEmptyRecords_whenValidListProvided() {
    when(userEventDao.excludeFieldsFromAll(any(), any(), any()))
      .thenReturn(Future.succeededFuture());
    when(userEventDao.deleteEmptyUpdateRecords(conn, TENANT_ID))
      .thenReturn(Future.succeededFuture());

    var result = exclusionHandler.onSettingChanged("[\"personal.email\",\"barcode\"]", conn, TENANT_ID);

    assertThat(result.succeeded()).isTrue();
    verify(userEventDao).excludeFieldsFromAll(Set.of("personal.email", "barcode"), conn, TENANT_ID);
    verify(userEventDao).deleteEmptyUpdateRecords(conn, TENANT_ID);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "[]", "not-json"})
  void onSettingChanged_shouldNotExclude_whenValueIsEmptyOrInvalid(String value) {
    var result = exclusionHandler.onSettingChanged(value, conn, TENANT_ID);

    assertThat(result.succeeded()).isTrue();
    verify(userEventDao, never()).excludeFieldsFromAll(any(), any(), any());
  }

  @Test
  void onSettingChanged_shouldNotExclude_whenValueIsNotString() {
    var result = exclusionHandler.onSettingChanged(42, conn, TENANT_ID);

    assertThat(result.succeeded()).isTrue();
    verify(userEventDao, never()).excludeFieldsFromAll(any(), any(), any());
  }

  @ParameterizedTest
  @MethodSource("isResponsibleCases")
  void isResponsible_shouldMatchOnlyUserExcludedFieldsSetting(String groupId, String settingKey, boolean expected) {
    assertThat(exclusionHandler.isResponsible(groupId, settingKey)).isEqualTo(expected);
  }

  static Stream<Arguments> isResponsibleCases() {
    return Stream.of(
      Arguments.of(USER_GROUP_ID, EXCLUDED_FIELDS_KEY, true),
      Arguments.of(USER_GROUP_ID, ENABLED_KEY, false),
      Arguments.of(SettingGroup.INVENTORY.getId(), EXCLUDED_FIELDS_KEY, false)
    );
  }
}
