package org.folio.builder.service;

public enum FeeFineDescriptions {
  BILLED("{\"userId\":\"6f36265e-722a-490a-b436-806e63af2ea7\",\"userBarcode\":\"693787594998493\",\"itemBarcode\":\"90000\",\"action\":\"Billed\",\"date\":\"2020-10-15T12:17:38.627Z\",\"servicePointId\":\"7c5abc9f-f3d7-4856-b8d7-6712462ca007\",\"source\":\"ADMINISTRATOR, DIKU\",\"feeFineId\":\"7ad9dfa0-6ee9-43ba-8db5-7a034ce05838\",\"feeFineOwner\":\"sample owner\",\"loanId\":\"0\",\"automated\":false,\"type\":\"manual charge\",\"paymentMethod\":\"cash\",\"amount\":10.0,\"balance\":0.0,\"comments\":\"STAFF : staff info \\n PATRON : patron info\"}",
    "Fee/Fine type: manual charge. Fee/Fine owner: sample owner. Amount: 10.00. manual. Additional information to staff: STAFF : staff info \n PATRON : patron info"),
  CANCELLED("{\"userId\":\"6f36265e-722a-490a-b436-806e63af2ea7\",\"userBarcode\":\"693787594998493\",\"itemBarcode\":\"90000\",\"action\":\"Cancelled as error\",\"date\":\"2020-10-15T12:17:38.627Z\",\"servicePointId\":\"7c5abc9f-f3d7-4856-b8d7-6712462ca007\",\"source\":\"ADMINISTRATOR, DIKU\",\"feeFineId\":\"7ad9dfa0-6ee9-43ba-8db5-7a034ce05838\",\"feeFineOwner\":\"sample owner\",\"loanId\":\"0\",\"automated\":false,\"type\":\"Paid fully\",\"paymentMethod\":\"cash\",\"amount\":10.0,\"balance\":0.0,\"comments\":\"STAFF : sample reason \\n PATRON : patron info\"}",
    "Amount: 10.00. Cancellation reason: sample reason. Additional information to patron: patron info."),
  PAID("{\"userId\":\"6f36265e-722a-490a-b436-806e63af2ea7\",\"userBarcode\":\"693787594998493\",\"itemBarcode\":\"90000\",\"action\":\"Paid fully\",\"date\":\"2020-10-15T12:17:38.627Z\",\"servicePointId\":\"7c5abc9f-f3d7-4856-b8d7-6712462ca007\",\"source\":\"ADMINISTRATOR, DIKU\",\"feeFineId\":\"7ad9dfa0-6ee9-43ba-8db5-7a034ce05838\",\"feeFineOwner\":\"sample owner\",\"loanId\":\"0\",\"automated\":false,\"type\":\"cash\",\"paymentMethod\":\"cash\",\"amount\":10.0,\"balance\":0.0,\"comments\":\"STAFF : staff info \\n PATRON : patron info\"}",
    "Fee/Fine type: cash. Amount: 10.00. Balance: 0.00. Payment method: cash. Additional information to staff: staff info. Additional information to patron: patron info."),
  WAIVED("{\"userId\":\"6f36265e-722a-490a-b436-806e63af2ea7\",\"userBarcode\":\"693787594998493\",\"itemBarcode\":\"90000\",\"action\":\"Waived fully\",\"date\":\"2020-10-15T12:17:38.627Z\",\"servicePointId\":\"7c5abc9f-f3d7-4856-b8d7-6712462ca007\",\"source\":\"ADMINISTRATOR, DIKU\",\"feeFineId\":\"7ad9dfa0-6ee9-43ba-8db5-7a034ce05838\",\"feeFineOwner\":\"sample owner\",\"loanId\":\"0\",\"automated\":false,\"type\":\"sample waive\",\"paymentMethod\":\"sample waive\",\"amount\":10.0,\"balance\":0.0,\"comments\":\"STAFF : staff info \\n PATRON : patron info\"}",
    "Fee/Fine type: sample waive. Amount: 10.00. Balance: 0.00. Waive reason: sample waive. Additional information to staff: staff info. Additional information to patron: patron info."),
  REFUND("{\"userId\":\"6f36265e-722a-490a-b436-806e63af2ea7\",\"userBarcode\":\"693787594998493\",\"itemBarcode\":\"90000\",\"action\":\"Refunded fully\",\"date\":\"2020-10-15T12:17:38.627Z\",\"servicePointId\":\"7c5abc9f-f3d7-4856-b8d7-6712462ca007\",\"source\":\"ADMINISTRATOR, DIKU\",\"feeFineId\":\"7ad9dfa0-6ee9-43ba-8db5-7a034ce05838\",\"feeFineOwner\":\"sample owner\",\"loanId\":\"0\",\"automated\":false,\"type\":\"sample refund\",\"paymentMethod\":\"sample refund\",\"amount\":10.0,\"balance\":0.0,\"comments\":\"STAFF : staff info \\n PATRON : patron info\"}",
    "Fee/Fine type: sample refund. Amount: 10.00. Balance: 0.00. Refund reason: sample refund. Additional information to staff: staff info. Additional information to patron: patron info."),
  TRANSFERRED("{\"userId\":\"6f36265e-722a-490a-b436-806e63af2ea7\",\"userBarcode\":\"693787594998493\",\"itemBarcode\":\"90000\",\"action\":\"Transferred fully\",\"date\":\"2020-10-15T12:17:38.627Z\",\"servicePointId\":\"7c5abc9f-f3d7-4856-b8d7-6712462ca007\",\"source\":\"ADMINISTRATOR, DIKU\",\"feeFineId\":\"7ad9dfa0-6ee9-43ba-8db5-7a034ce05838\",\"feeFineOwner\":\"sample owner\",\"loanId\":\"0\",\"automated\":false,\"type\":\"sample transfer\",\"paymentMethod\":\"sample transfer\",\"amount\":10.0,\"balance\":0.0,\"comments\":\"STAFF : staff info \\n PATRON : patron info\"}",
    "Fee/Fine type: sample transfer. Amount: 10.00. Balance: 0.00. Transfer account: sample transfer. Additional information to staff: staff info. Additional information to patron: patron info."),
  STAFF_INFO("{\"userId\":\"6f36265e-722a-490a-b436-806e63af2ea7\",\"userBarcode\":\"693787594998493\",\"itemBarcode\":\"90000\",\"action\":\"Staff information only added\",\"date\":\"2020-10-15T12:17:38.627Z\",\"servicePointId\":\"7c5abc9f-f3d7-4856-b8d7-6712462ca007\",\"source\":\"ADMINISTRATOR, DIKU\",\"feeFineId\":\"7ad9dfa0-6ee9-43ba-8db5-7a034ce05838\",\"feeFineOwner\":\"sample owner\",\"loanId\":\"0\",\"automated\":false,\"type\":\"sample transfer\",\"paymentMethod\":\"sample transfer\",\"amount\":10.0,\"balance\":0.0,\"comments\":\"STAFF : staff info\"}",
    "staff info");

  private String payload;
  private String expected;

  FeeFineDescriptions(String payload, String expected) {
    this.payload = payload;
    this.expected = expected;
  }

  public String getPayload() {
    return payload;
  }

  public String getExpected() {
    return expected;
  }
}
