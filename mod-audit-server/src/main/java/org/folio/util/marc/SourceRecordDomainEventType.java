package org.folio.util.marc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SourceRecordDomainEventType {

  SOURCE_RECORD_CREATED("SOURCE_RECORD_CREATED"),
  SOURCE_RECORD_UPDATED("SOURCE_RECORD_UPDATED"),
  SOURCE_RECORD_DELETED("SOURCE_RECORD_DELETED");

  private final String value;

  SourceRecordDomainEventType(String value) {
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
  public static SourceRecordDomainEventType fromValue(String value) {
    for (var eventType : SourceRecordDomainEventType.values()) {
      if (eventType.value.equals(value)) {
        return eventType;
      }
    }
    throw new IllegalArgumentException("No matching SourceRecordDomainEventType for value: " + value);
  }
}
