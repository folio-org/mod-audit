package org.folio.util.inventory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum InventoryEventType {

  CREATE("CREATE"),
  UPDATE("UPDATE"),
  DELETE("DELETE"),
  UNKNOWN("UNKNOWN");

  private String value;

  InventoryEventType(String value) {
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
  public static InventoryEventType fromValue(String value) {
    for (var eventType : InventoryEventType.values()) {
      if (eventType.value.equals(value)) {
        return eventType;
      }
    }
    return UNKNOWN;
  }
}
