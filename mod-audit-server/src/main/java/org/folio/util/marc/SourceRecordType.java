package org.folio.util.marc;

public enum SourceRecordType {

  MARC_BIB("MARC_BIB"),
  MARC_AUTHORITY("MARC_AUTHORITY");

  private final String value;

  SourceRecordType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
