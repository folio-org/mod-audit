package org.folio.util;

public enum EventType {
  LOG_RECORD_EVENT("Created log record event");

  EventType(String description) {
    this.description = description;
  }

  private final String description;

  public String description() {
    return description;
  }
}
