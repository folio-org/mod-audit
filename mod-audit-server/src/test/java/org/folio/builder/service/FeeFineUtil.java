package org.folio.builder.service;

public enum FeeFineUtil {

  MANUAL("payloads/fee_fine_billed.json","manual"),
  AUTOMATED("payloads/fee_fine_billed_automated.json","automated");
  private String fileName;
  private String type;

  FeeFineUtil(String fileName, String type) {
    this.fileName = fileName;
    this.type = type;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
