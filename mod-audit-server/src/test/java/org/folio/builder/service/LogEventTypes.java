package org.folio.builder.service;

public enum LogEventTypes {
  CHECK_IN_EVENT("CHECK_IN_EVENT", CheckInRecordBuilderService.class),
  CHECK_OUT_EVENT("CHECK_OUT_EVENT", CheckOutRecordBuilderService.class),
  MANUAL_BLOCK_CREATED("MANUAL_BLOCK_CREATED_EVENT", ManualBlockRecordBuilderService.class),
  MANUAL_BLOCK_MODIFIED("MANUAL_BLOCK_MODIFIED_EVENT", ManualBlockRecordBuilderService.class),
  MANUAL_BLOCK_DELETED("MANUAL_BLOCK_DELETED_EVENT", ManualBlockRecordBuilderService.class),
  LOAN("LOAN", LoanRecordBuilderService.class),
  NOTICE("NOTICE", NoticeRecordBuilderService.class),
  FEE_FINE("FEE_FINE", FeeFineRecordBuilderService.class),
  REQUEST_CREATED("REQUEST_CREATED_EVENT", RequestRecordBuilderService.class),
  REQUEST_UPDATED("REQUEST_UPDATED_EVENT", RequestRecordBuilderService.class),
  REQUEST_MOVED("REQUEST_MOVED_EVENT", RequestRecordBuilderService.class),
  REQUEST_REORDERED("REQUEST_REORDERED_EVENT", RequestRecordBuilderService.class);

  private String value;
  private Class clazz;

  LogEventTypes(String value, Class clazz) {
    this.value = value;
    this.clazz = clazz;
  }

  public String getValue() {
    return value;
  }

  public Class getClazz() {
    return clazz;
  }
}
