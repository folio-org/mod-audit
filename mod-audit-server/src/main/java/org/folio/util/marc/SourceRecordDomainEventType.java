package org.folio.util.marc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;


/**
 * Enum representing the types of domain events for a source record.
 *
 * This enumeration defines various types of actions or state changes
 * that can be applied to a source record. These events may include:
 * - Creation of a source record
 * - Updating of a source record
 * - Deletion of a source record
 * - An unknown or undefined event type
 *
 * Each constant of this enum has an associated string value representation
 * that can be retrieved or used to parse the event type from a string input.
 *
 * Methods:
 * - getValue(): Returns the string representation of the event type.
 * - toString(): Overrides the default toString method to return the string value.
 * - fromValue(String): Factory method to map a string value to the corresponding enum constant.
 *                      Returns UNKNOWN if the value does not match any predefined constants.
 */
public enum SourceRecordDomainEventType {

  SOURCE_RECORD_CREATED("SOURCE_RECORD_CREATED"),
  SOURCE_RECORD_UPDATED("SOURCE_RECORD_UPDATED"),
  SOURCE_RECORD_DELETED("SOURCE_RECORD_DELETED"),
  UNKNOWN("UNKNOWN");

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
    return UNKNOWN;
  }
}
