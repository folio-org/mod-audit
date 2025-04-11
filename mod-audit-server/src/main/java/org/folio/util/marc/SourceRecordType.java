package org.folio.util.marc;

import lombok.Getter;

/**
 * Enum representing the type of source record in the MARC (Machine-Readable Cataloging) format.
 * <p>
 * SourceRecordType defines constants for the classification of a source record,
 * enabling differentiation between bibliographic and authority records in library systems.
 * It is commonly used in conjunction with classes handling MARC record processing.
 * <p>
 * Enum values:
 * - MARC_BIB: Represents a bibliographic record.
 * - MARC_AUTHORITY: Represents an authority record.
 * - MARC_HOLDING: Represents a marc-holding record.
 */
@Getter
public enum SourceRecordType {

  MARC_BIB("MARC_BIB"),
  MARC_AUTHORITY("MARC_AUTHORITY"),
  MARC_HOLDING("MARC_HOLDING");

  private final String value;

  SourceRecordType(String value) {
    this.value = value;
  }

}
