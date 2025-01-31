package org.folio.services.configuration;

import org.folio.exception.ValidationException;
import org.folio.rest.jaxrs.model.Setting;
import org.springframework.stereotype.Service;

@Service
public class SettingValidationService {

  public void validateSetting(Setting setting, String groupId, String settingKey) {
    if (setting.getGroupId() == null || !setting.getGroupId().equals(groupId)
        || setting.getKey() == null || !setting.getKey().equals(settingKey)) {
      throw new ValidationException("Setting group id or key does not match the path parameters");
    }
    validateSettingValue(setting);
  }

  private void validateSettingValue(Setting setting) {
    switch (setting.getType()) {
      case STRING -> validateType(setting.getValue(), String.class, "Setting value should be a string");
      case INTEGER -> validateType(setting.getValue(), Integer.class, "Setting value should be an integer");
      case BOOLEAN -> validateType(setting.getValue(), Boolean.class, "Setting value should be a boolean");
    }
  }

  private <T> void validateType(Object value, Class<T> type, String errorMessage) {
    if (!type.isInstance(value)) {
      throw new ValidationException(errorMessage);
    }
  }
}
