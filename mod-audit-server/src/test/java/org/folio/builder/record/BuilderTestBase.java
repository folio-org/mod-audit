package org.folio.builder.record;

import static org.folio.util.JsonPropertyFetcher.getArrayProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
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

import org.folio.TestBase;
import org.folio.rest.jaxrs.model.LogRecord;
import org.junit.jupiter.api.BeforeAll;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class BuilderTestBase extends TestBase {

  static LogRecordBuilder checkInRecordBuilder, checkOutRecordBuilder;

  @BeforeAll
  public static void setUp() {
    checkInRecordBuilder = new CheckInRecordBuilder();
    checkOutRecordBuilder = new CheckOutRecordBuilder();
  }

  public void validateRequestStatusChangedContent(JsonObject payload, LogRecord requestStatusChanged) {
    JsonArray requests = getArrayProperty(payload, REQUESTS);
    JsonObject request = requests.getJsonObject(0);
    assertThat(requestStatusChanged.getLinkToIds().getRequestId(), equalTo(getProperty(request, REQUEST_ID)));
  }

  public void validateAdditionalContent(JsonObject payload, LogRecord itemCheckedOutRecord) {
    assertThat(itemCheckedOutRecord.getLinkToIds().getUserId(), equalTo(getProperty(payload, USER_ID)));
    assertThat(itemCheckedOutRecord.getItems().get(0).getItemId(), equalTo(getProperty(payload, ITEM_ID)));
    assertThat(itemCheckedOutRecord.getItems().get(0).getLoanId(), equalTo(getProperty(payload, LOAN_ID)));
  }

  public void validateBaseContent(JsonObject payload, LogRecord itemCheckedOutRecord) {
    assertThat(itemCheckedOutRecord.getUserBarcode(), equalTo(getProperty(payload, USER_BARCODE)));
    assertThat(itemCheckedOutRecord.getItems().get(0).getItemBarcode(), equalTo(getProperty(payload, ITEM_BARCODE)));
    assertThat(itemCheckedOutRecord.getSource(), equalTo(getProperty(payload, SOURCE)));
    assertThat(itemCheckedOutRecord.getServicePointId(), equalTo(getProperty(payload, SERVICE_POINT_ID)));
  }
}
