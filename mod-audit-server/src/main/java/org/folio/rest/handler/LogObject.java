package org.folio.rest.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public enum LogObject {
  FEE_FINE("Fee/Fine", "fees_fines"),
  ITEM_BLOCK("Item Block", "item_blocks"),
  LOAN("Loan", "loans"),
  MANUAL_BLOCK("Manual Block", "manual_blocks"),
  NOTICE("Notice", "notices"),
  PATRON_BLOCK("Patron Block", "patron_blocks"),
  REQUEST("Request", "requests");

  private final String objectName;
  private final String tableName;
  private static final Map<String, LogObject> LOG_OBJECTS = new HashMap<>();

  static {
    for (LogObject obj: values()) {
      LOG_OBJECTS.put(obj.objectName, obj);
    }
  }

  LogObject(String objectName, String tableName) {
    this.objectName = objectName;
    this.tableName = tableName;
  }

  public static LogObject fromName(String objectName) {
    LogObject logObject = LOG_OBJECTS.get(objectName);
    if (Objects.nonNull(logObject)) {
      return logObject;
    }
    throw new IllegalArgumentException(objectName);
  }

  public String tableName() {
    return tableName;
  }
}
