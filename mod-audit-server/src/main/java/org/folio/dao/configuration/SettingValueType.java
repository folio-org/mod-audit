package org.folio.dao.configuration;

import java.util.HashMap;
import java.util.Map;

public enum SettingValueType {

  STRING("STRING"),
  INTEGER("INTEGER"),
  BOOLEAN("BOOLEAN");

  private static final Map<String, SettingValueType> CONSTANTS = new HashMap<>();

  static {
    for (SettingValueType c : values()) {
      CONSTANTS.put(c.value, c);
    }
  }

  private final String value;

  SettingValueType(String value) {
    this.value = value;
  }

  public static SettingValueType fromValue(String value) {
    var constant = CONSTANTS.get(value);
    if (constant == null) {
      throw new IllegalArgumentException("Value '%s' is not a valid SettingValueType".formatted(value));
    } else {
      return constant;
    }
  }

  @Override
  public String toString() {
    return this.value;
  }

  public String value() {
    return this.value;
  }
}
