package org.folio.builder.service;

public enum LogEventTypes {
  CHECK_IN_EVENT("CHECK_IN_EVENT", CheckInRecordBuilder.class),
  CHECK_OUT_EVENT("CHECK_OUT_EVENT", CheckOutRecordBuilder.class),
  MANUAL_BLOCK_CREATED("MANUAL_BLOCK_CREATED_EVENT", ManualBlockRecordBuilder.class),
  MANUAL_BLOCK_MODIFIED("MANUAL_BLOCK_MODIFIED_EVENT", ManualBlockRecordBuilder.class),
  MANUAL_BLOCK_DELETED("MANUAL_BLOCK_DELETED_EVENT", ManualBlockRecordBuilder.class),
  LOAN("LOAN", LoanRecordBuilder.class),
  NOTICE("NOTICE", NoticeSuccessRecordBuilder.class),
  NOTICE_ERROR("NOTICE_ERROR", NoticeErrorRecordBuilder.class),
  FEE_FINE("FEE_FINE", FeeFineRecordBuilder.class),
  REQUEST_CREATED("REQUEST_CREATED_EVENT", RequestRecordBuilder.class),
  REQUEST_UPDATED("REQUEST_UPDATED_EVENT", RequestRecordBuilder.class),
  REQUEST_MOVED("REQUEST_MOVED_EVENT", RequestRecordBuilder.class),
  REQUEST_REORDERED("REQUEST_REORDERED_EVENT", RequestRecordBuilder.class);

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
