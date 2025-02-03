package org.folio.services.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.folio.CopilotGenerated;
import org.folio.exception.ValidationException;
import org.folio.rest.jaxrs.model.Setting;
import org.folio.rest.jaxrs.model.Setting.Type;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@UnitTest
@CopilotGenerated
class SettingValidationServiceTest {

  private static final String GROUP_ID = "group1";
  private static final String SETTING_KEY = "key1";

  private SettingValidationService settingValidationService;

  @BeforeEach
  void setUp() {
    settingValidationService = new SettingValidationService();
  }

  @Test
  void validateSetting_shouldThrowException_whenGroupIdDoesNotMatch() {
    Setting setting = new Setting().withGroupId(GROUP_ID).withKey(SETTING_KEY);
    String groupId = "group2";

    ValidationException exception = assertThrows(ValidationException.class, () ->
      settingValidationService.validateSetting(setting, groupId, SETTING_KEY)
    );

    assertEquals("Setting group id or key does not match the path parameters", exception.getMessage());
  }

  @Test
  void validateSetting_shouldThrowException_whenKeyDoesNotMatch() {
    Setting setting = new Setting().withGroupId(GROUP_ID).withKey(SETTING_KEY);
    String settingKey = "key2";

    ValidationException exception = assertThrows(ValidationException.class, () ->
      settingValidationService.validateSetting(setting, GROUP_ID, settingKey)
    );

    assertEquals("Setting group id or key does not match the path parameters", exception.getMessage());
  }

  @Test
  void validateSetting_shouldNotThrowException_whenGroupIdAndKeyMatch() {
    Setting setting = getSetting(Type.STRING, "value");

    assertDoesNotThrow(() -> settingValidationService.validateSetting(setting, GROUP_ID, SETTING_KEY));
  }

  @Test
  void validateSettingValue_shouldThrowException_whenTypeIsStringAndValueIsNotString() {
    var setting = getSetting(Type.STRING, 123);

    ValidationException exception = assertThrows(ValidationException.class, () ->
      settingValidationService.validateSetting(setting, GROUP_ID, SETTING_KEY)
    );

    assertEquals("Setting value should be a string", exception.getMessage());
  }

  @Test
  void validateSettingValue_shouldThrowException_whenTypeIsIntegerAndValueIsNotInteger() {
    var setting = getSetting(Type.INTEGER, "value");

    ValidationException exception = assertThrows(ValidationException.class, () ->
      settingValidationService.validateSetting(setting, GROUP_ID, SETTING_KEY)
    );

    assertEquals("Setting value should be an integer", exception.getMessage());
  }

  @Test
  void validateSettingValue_shouldThrowException_whenTypeIsBooleanAndValueIsNotBoolean() {
    var setting = getSetting(Type.BOOLEAN, "value");

    ValidationException exception = assertThrows(ValidationException.class, () ->
      settingValidationService.validateSetting(setting, GROUP_ID, SETTING_KEY)
    );

    assertEquals("Setting value should be a boolean", exception.getMessage());
  }

  @Test
  void validateSettingValue_shouldNotThrowException_whenTypeAndValueMatch() {
    var settingString = getSetting(Type.STRING, "value");
    var settingInteger = getSetting(Type.INTEGER, 123);
    var settingBoolean = getSetting(Type.BOOLEAN, true);

    assertDoesNotThrow(() -> settingValidationService.validateSetting(settingString, GROUP_ID, SETTING_KEY));
    assertDoesNotThrow(() -> settingValidationService.validateSetting(settingInteger, GROUP_ID, SETTING_KEY));
    assertDoesNotThrow(() -> settingValidationService.validateSetting(settingBoolean, GROUP_ID, SETTING_KEY));
  }

  private Setting getSetting(Type type, Object value) {
    return new Setting().withGroupId(GROUP_ID).withKey(SETTING_KEY).withType(type).withValue(value);
  }
}