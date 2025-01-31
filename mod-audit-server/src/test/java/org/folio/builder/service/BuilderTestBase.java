package org.folio.builder.service;

import static org.folio.TestSuite.isInitialized;
import static org.folio.builder.LogRecordBuilderResolver.CHECK_OUT_EVENT;
import static org.folio.builder.LogRecordBuilderResolver.CHECK_OUT_THROUGH_OVERRIDE_EVENT;
import static org.folio.util.JsonPropertyFetcher.getArrayProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.HOLDINGS_RECORD_ID;
import static org.folio.util.LogEventPayloadField.INSTANCE_ID;
import static org.folio.util.LogEventPayloadField.ITEM_BARCODE;
import static org.folio.util.LogEventPayloadField.ITEM_ID;
import static org.folio.util.LogEventPayloadField.LOAN_ID;
import static org.folio.util.LogEventPayloadField.REQUESTS;
import static org.folio.util.LogEventPayloadField.REQUEST_ID;
import static org.folio.util.LogEventPayloadField.SERVICE_POINT_ID;
import static org.folio.util.LogEventPayloadField.SOURCE;
import static org.folio.util.LogEventPayloadField.USER_BARCODE;
import static org.folio.util.LogEventPayloadField.USER_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.HashMap;

import org.folio.TestSuite;
import org.folio.rest.jaxrs.model.LogRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class BuilderTestBase {

  static LogRecordBuilder checkInRecordBuilder, checkOutRecordBuilder, checkOutThroughOverrideRecordBuilder, manualBlockRecordBuilder, feeFineRecordBuilder,
      noticeRecordBuilder, noticeErrorRecordBuilder, loanRecordBuilder, requestLogRecordBuilder;

  @BeforeAll
  public static void setUp() {
    checkInRecordBuilder = new CheckInRecordBuilder(new HashMap<>(), TestSuite.getVertx().getOrCreateContext());
    checkOutRecordBuilder = new CheckOutRecordBuilder(new HashMap<>(), TestSuite.getVertx().getOrCreateContext(), CHECK_OUT_EVENT);
    checkOutThroughOverrideRecordBuilder = new CheckOutRecordBuilder(new HashMap<>(), TestSuite.getVertx().getOrCreateContext(), CHECK_OUT_THROUGH_OVERRIDE_EVENT);
    manualBlockRecordBuilder = new ManualBlockRecordBuilder(new HashMap<>(), TestSuite.getVertx().getOrCreateContext());
    feeFineRecordBuilder = new FeeFineRecordBuilder(new HashMap<>(), TestSuite.getVertx().getOrCreateContext());
    noticeRecordBuilder = new NoticeSuccessRecordBuilder(new HashMap<>(), TestSuite.getVertx().getOrCreateContext());
    noticeErrorRecordBuilder = new NoticeErrorRecordBuilder(new HashMap<>(), TestSuite.getVertx().getOrCreateContext());
    loanRecordBuilder = new LoanRecordBuilder(new HashMap<>(), TestSuite.getVertx().getOrCreateContext());
    requestLogRecordBuilder = new RequestRecordBuilder(new HashMap<>(), TestSuite.getVertx().getOrCreateContext());
  }

  @AfterAll
  public static void tearDown() {
    if (isInitialized) {
      TestSuite.globalTearDown();
    }
  }

  public void validateRequestStatusChangedContent(JsonObject payload, LogRecord requestStatusChanged) {
    JsonArray requests = getArrayProperty(payload, REQUESTS);
    JsonObject request = requests.getJsonObject(0);
    assertThat(requestStatusChanged.getLinkToIds()
      .getRequestId(), equalTo(getProperty(request, REQUEST_ID)));
  }

  public void validateAdditionalContent(JsonObject payload, LogRecord itemCheckedOutRecord) {
    assertThat(itemCheckedOutRecord.getLinkToIds().getUserId(), equalTo(getProperty(payload, USER_ID)));
    assertThat(itemCheckedOutRecord.getItems().get(0).getItemId(), equalTo(getProperty(payload, ITEM_ID)));
    assertThat(itemCheckedOutRecord.getItems().get(0).getLoanId(), equalTo(getProperty(payload, LOAN_ID)));
  }

  public void validateBaseContent(JsonObject payload, LogRecord itemCheckedOutRecord) {
    assertThat(itemCheckedOutRecord.getUserBarcode(), equalTo(getProperty(payload, USER_BARCODE)));
    assertThat(itemCheckedOutRecord.getItems().get(0).getItemBarcode(), equalTo(getProperty(payload, ITEM_BARCODE)));
    assertThat(itemCheckedOutRecord.getItems().get(0).getHoldingId(), equalTo(getProperty(payload, HOLDINGS_RECORD_ID)));
    assertThat(itemCheckedOutRecord.getItems().get(0).getInstanceId(), equalTo(getProperty(payload, INSTANCE_ID)));
    assertThat(itemCheckedOutRecord.getSource(), equalTo(getProperty(payload, SOURCE)));
    assertThat(itemCheckedOutRecord.getServicePointId(), equalTo(getProperty(payload, SERVICE_POINT_ID)));
  }
}
