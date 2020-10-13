package org.folio.builder.record;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.jaxrs.model.LogRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.stream.Collectors.groupingBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class RequestRecordBuilderTest extends BuilderTestBase {

  private static final Logger logger = LoggerFactory.getLogger(CheckInRecordBuilderTest.class);

  @Test
  public void requestCreatedTest() {

    logger.info("===== Test check-out log records builder =====");

    JsonObject payload = new JsonObject(getFile(REQUEST_CREATED_PAYLOAD_JSON));

    List<LogRecord> records = requestRecordBuilder.buildLogRecord(payload);

    assertThat(records, hasSize(1));
//    assertThat(records.get(LogRecord.Object.REQUEST)
//      .get(LogRecord.Action.CHECKED_OUT), hasSize(1));
//    assertThat(records.get(LogRecord.Object.REQUEST)
//      .get(LogRecord.Action.REQUEST_STATUS_CHANGED), hasSize(1));
//
//    LogRecord loanCheckedOutRecord = records.get(LogRecord.Object.LOAN)
//      .get(LogRecord.Action.CHECKED_OUT).get(0);
//
//    validateBaseContent(payload, loanCheckedOutRecord);
//    validateAdditionalContent(payload, loanCheckedOutRecord);
//    assertThat(loanCheckedOutRecord.getDescription(), equalTo("Checked out to proxy: "
//      + (Objects.nonNull(getProperty(payload, PROXY_BARCODE)) ? getProperty(payload, PROXY_BARCODE) : "no") + DOT_MSG));
//
//    LogRecord requestStatusChangedRecord = records.get(LogRecord.Object.REQUEST)
//      .get(LogRecord.Action.REQUEST_STATUS_CHANGED).get(0);
//
//    validateBaseContent(payload, requestStatusChangedRecord);
//    validateRequestStatusChangedContent(payload, requestStatusChangedRecord);
//
//    JsonArray requests = getArrayProperty(payload, REQUESTS);
//    JsonObject request = requests.getJsonObject(0);
//
//    String requestType = getProperty(request, REQUEST_TYPE);
//    String oldRequestStatus = getProperty(request, OLD_REQUEST_STATUS);
//    String newRequestStatus = getProperty(request, NEW_REQUEST_STATUS);
//
//    assertThat(requestStatusChangedRecord.getDescription(),
//      equalTo(String.format("Type: %s. New request status: %s (from: %s).", requestType, newRequestStatus, oldRequestStatus)));


  }

  @Test
  public void requestEditTest() {

  }

  @Test
  public void requestMovedTest() {

  }

  @Test
  public void requestReorderedTest() {

  }
}
