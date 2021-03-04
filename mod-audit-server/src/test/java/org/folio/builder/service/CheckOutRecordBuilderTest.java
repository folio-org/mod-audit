package org.folio.builder.service;

import static java.util.stream.Collectors.groupingBy;
import static org.folio.builder.description.Descriptions.DOT_MSG;
import static org.folio.util.JsonPropertyFetcher.getArrayProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.NEW_REQUEST_STATUS;
import static org.folio.util.LogEventPayloadField.OLD_REQUEST_STATUS;
import static org.folio.util.LogEventPayloadField.PROXY_BARCODE;
import static org.folio.util.LogEventPayloadField.REQUESTS;
import static org.folio.util.LogEventPayloadField.REQUEST_TYPE;
import static org.folio.utils.TenantApiTestUtil.CHECK_OUT_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.getFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.LogRecord;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class CheckOutRecordBuilderTest extends BuilderTestBase {

  private static final Logger logger = LogManager.getLogger();

  @Test
  public void checkOutTest() throws Exception {
    logger.info("===== Test check-out log records builder =====");

    JsonObject payload = new JsonObject(getFile(CHECK_OUT_PAYLOAD_JSON));

    Map<LogRecord.Object, Map<LogRecord.Action, List<LogRecord>>> records = checkOutRecordBuilder.buildLogRecord(payload)
      .get().stream()
      .collect(groupingBy(LogRecord::getObject, groupingBy(LogRecord::getAction)));

    assertThat(records.entrySet(), hasSize(2));
    assertThat(records.get(LogRecord.Object.LOAN)
      .get(LogRecord.Action.CHECKED_OUT), hasSize(1));
    assertThat(records.get(LogRecord.Object.REQUEST)
      .get(LogRecord.Action.REQUEST_STATUS_CHANGED), hasSize(1));

    LogRecord loanCheckedOutRecord = records.get(LogRecord.Object.LOAN)
      .get(LogRecord.Action.CHECKED_OUT).get(0);

    validateBaseContent(payload, loanCheckedOutRecord);
    validateAdditionalContent(payload, loanCheckedOutRecord);
    assertThat(loanCheckedOutRecord.getDescription(), equalTo("Checked out to proxy: "
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
