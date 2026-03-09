package org.folio.util.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UserEventType {

  CREATED("CREATED"),
  UPDATED("UPDATED"),
  DELETED("DELETED"),
  UNKNOWN("UNKNOWN");

  private final String value;

  UserEventType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static UserEventType fromValue(String value) {
    for (var eventType : UserEventType.values()) {
      if (eventType.value.equals(value)) {
        return eventType;
      }
    }
    return UNKNOWN;
  }
}
