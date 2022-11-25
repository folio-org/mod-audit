package org.folio.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum OrderEventTypes {

  ORDER_ERROR("DI_ERROR"),
  ORDER_TYPE_CHANGED("ORDER_TYPE_CHANGED");

  private final String value;
  private static final Map<String, OrderEventTypes> CONSTANTS = new HashMap();

  OrderEventTypes(String value) {
    this.value = value;
  }


  public String toString() {
    return this.value;
  }

  @JsonValue
  public String value() {
    return this.value;
  }

  @JsonCreator
  public static OrderEventTypes fromValue(String value) {
    OrderEventTypes constant = (OrderEventTypes)CONSTANTS.get(value);
    if (constant == null) {
      throw new IllegalArgumentException(value);
    } else {
      return constant;
    }
  }

  static {
    OrderEventTypes[] var0 = values();
    int var1 = var0.length;

    for(int var2 = 0; var2 < var1; ++var2) {
      OrderEventTypes c = var0[var2];
      CONSTANTS.put(c.value, c);
    }

  }
}
