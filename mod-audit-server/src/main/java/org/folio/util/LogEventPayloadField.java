package org.folio.util;

public enum LogEventPayloadField {
  LOG_EVENT_TYPE("logEventType"),
  PAYLOAD("payload"),
  CREATED("created"),
  ORIGINAL("original"),
  REORDERED("reordered"),
  UPDATED("updated"),
  STATUS("status"),
  CLAIMED_RETURNED_RESOLUTION("claimedReturnedResolution"),
  SERVICE_POINT_ID("servicePointId"),
  REQUESTS("requests"),
  ITEM("item"),
  ITEM_ID("itemId"),
  ITEM_BARCODE("itemBarcode"),
  HOLDINGS_RECORD_ID("holdingsRecordId"),
  INSTANCE_ID("instanceId"),
  ITEM_STATUS_NAME("itemStatusName"),
  DESTINATION_SERVICE_POINT("destinationServicePoint"),
  SOURCE("source"),
  LOAN_ID("loanId"),
  IS_LOAN_CLOSED("isLoanClosed"),
  SYSTEM_RETURN_DATE("systemReturnDate"),
  RETURN_DATE("returnDate"),
  DUE_DATE("dueDate"),
  USER_ID("userId"),
  USER_BARCODE("userBarcode"),
  PROXY_BARCODE("proxyBarcode"),
  REQUEST_ID("id"),
  OLD_REQUEST_STATUS("oldRequestStatus"),
  NEW_REQUEST_STATUS("newRequestStatus"),
  PICK_UP_SERVICE_POINT("pickUpServicePoint"),
  REQUEST_TYPE("requestType"),
  REQUEST_EXPIRATION_DATE("requestExpirationDate"),
  REQUEST_ADDRESS_TYPE("addressType"),
  REQUEST_REASON_FOR_CANCELLATION("addressType"),
  REQUEST_POSITION("position"),
  REQUEST_PREVIOUS_POSITION("previousPosition"),
  REQUEST_FULFILMENT_PREFERENCE("fulfilmentPreference"),
  REQUEST_SERVICE_POINT("servicePoint"),
  REQUEST_PICKUP_SERVICE_POINT("pickupServicePoint"),
  REQUESTER("requester"),
  BARCODE("barcode"),
  MANUAL_BLOCK_BORROWING("borrowing"),
  MANUAL_BLOCK_RENEWALS("renewals"),
  MANUAL_BLOCK_REQUESTS("requests"),
  MANUAL_BLOCK_DESCRIPTION("desc"),
  MANUAL_BLOCK_STAFF_INFORMATION("staffInformation"),
  MANUAL_BLOCK_MESSAGE_TO_PATRON("patronMessage"),
  MANUAL_BLOCK_EXPIRATION_DATE("expirationDate");


  private final String value;

  LogEventPayloadField(String value) {
    this.value = value;
  }

  public String value() {
    return this.value;
  }
}
