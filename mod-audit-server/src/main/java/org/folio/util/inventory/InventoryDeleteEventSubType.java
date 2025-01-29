package org.folio.util.inventory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum InventoryDeleteEventSubType {
  SOFT_DELETE("SOFT_DELETE"),
  UNKNOWN("UNKNOWN");

  private String value;

  InventoryDeleteEventSubType(String value) {
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
  public static InventoryDeleteEventSubType fromValue(String value) {
    if (SOFT_DELETE.value.equals(value)) {
      return SOFT_DELETE;
    } else {
      return UNKNOWN;
    }
  }
}
