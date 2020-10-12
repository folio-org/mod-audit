package org.folio.builder.record;

import static java.util.stream.Collectors.groupingBy;
import static org.folio.builder.description.Descriptions.DOT_MSG;
import static org.folio.util.JsonPropertyFetcher.getArrayProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.NEW_REQUEST_STATUS;
import static org.folio.util.LogEventPayloadField.OLD_REQUEST_STATUS;
import static org.folio.util.LogEventPayloadField.PROXY_BARCODE;
import static org.folio.util.LogEventPayloadField.REQUESTS;
import static org.folio.util.LogEventPayloadField.REQUEST_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.folio.rest.jaxrs.model.LogRecord;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class CheckOutRecordBuilderTestBase extends BuilderTestBase {

  private static final Logger logger = LoggerFactory.getLogger(CheckInRecordBuilderTestBase.class);

  @Test
  public void checkOutTest() {
    logger.info("===== Test check-out log records builder =====");

    JsonObject payload = new JsonObject(getFile(CHECK_OUT_PAYLOAD_JSON));

    Map<LogRecord.Object, Map<LogRecord.Action, List<LogRecord>>> records = checkOutRecordBuilder.buildLogRecord(payload)
      .stream()
      .collect(groupingBy(LogRecord::getObject, groupingBy(LogRecord::getAction)));

    assertThat(records.entrySet(), hasSize(2));
    assertThat(records.get(LogRecord.Object.ITEM)
      .get(LogRecord.Action.CHECKED_OUT), hasSize(1));
    assertThat(records.get(LogRecord.Object.REQUEST)
      .get(LogRecord.Action.REQUEST_STATUS_CHANGED), hasSize(1));

    LogRecord itemCheckedOutRecord = records.get(LogRecord.Object.ITEM)
      .get(LogRecord.Action.CHECKED_OUT).get(0);

    validateBaseContent(payload, itemCheckedOutRecord);
    validateAdditionalContent(payload, itemCheckedOutRecord);
    assertThat(itemCheckedOutRecord.getDescription(), equalTo("Checked out to proxy: "
        + (Objects.nonNull(getProperty(payload, PROXY_BARCODE)) ? getProperty(payload, PROXY_BARCODE) : "no") + DOT_MSG));

    LogRecord requestStatusChangedRecord = records.get(LogRecord.Object.REQUEST)
      .get(LogRecord.Action.REQUEST_STATUS_CHANGED).get(0);

    validateBaseContent(payload, requestStatusChangedRecord);
    validateRequestStatusChangedContent(payload, requestStatusChangedRecord);

    JsonArray requests = getArrayProperty(payload, REQUESTS);
    JsonObject request = requests.getJsonObject(0);

    String requestType = getProperty(request, REQUEST_TYPE);
    String oldRequestStatus = getProperty(request, OLD_REQUEST_STATUS);
    String newRequestStatus = getProperty(request, NEW_REQUEST_STATUS);

    assertThat(requestStatusChangedRecord.getDescription(),
        equalTo(String.format("Type: %s. New request status: %s (from: %s).", requestType, newRequestStatus, oldRequestStatus)));
  }
}
