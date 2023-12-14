package org.folio.util;

public enum LogEventPayloadField {
  ID("id"),
  ACCOUNT_ID("accountId"),
  AUTOMATED("automated"),
  NOTICE_POLICY_ID("noticePolicyId"),
  TRIGGERING_EVENT("triggeringEvent"),
  TEMPLATE_ID("templateId"),
  TEMPLATE_NAME("templateName"),
  NAME("name"),
  FIRST_NAME("firstName"),
  LAST_NAME("lastName"),
  PERSONAL_NAME("personalName"),
  PERSONAL("personal"),
  UPDATED_BY_USER_ID("updatedByUserId"),
  COMMENTS("comments"),
  PAYMENT_METHOD("paymentMethod"),
  BALANCE("balance"),
  AMOUNT("amount"),
  METADATA("metadata"),
  FEE_FINE_OWNER("feeFineOwner"),
  TYPE("type"),
  DESCRIPTION("description"),
  ACTION("action"),
  DATE("date"),
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
  ITEMS("items"),
  ITEM_ID("itemId"),
  ITEM_BARCODE("itemBarcode"),
  HOLDINGS_RECORD_ID("holdingsRecordId"),
  INSTANCE_ID("instanceId"),
  ITEM_STATUS_NAME("itemStatusName"),
  ZONE_ID("zoneId"),
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
  REQUEST_REASON_FOR_CANCELLATION_ID("cancellationReasonId"),
  REQUEST_POSITION("position"),
  REQUEST_PREVIOUS_POSITION("previousPosition"),
  REQUEST_FULFILMENT_PREFERENCE("fulfilmentPreference"),
  REQUEST_SERVICE_POINT("servicePoint"),
  REQUEST_PICKUP_SERVICE_POINT("pickupServicePoint"),
  REQUEST_PICKUP_SERVICE_POINT_ID("pickupServicePointId"),
  REQUESTER("requester"),
  REQUESTER_ID("requesterId"),
  BARCODE("barcode"),
  ERROR_MESSAGE("errorMessage");


  private final String value;

  LogEventPayloadField(String value) {
    this.value = value;
  }

  public String value() {
    return this.value;
  }
}
