package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.folio.rest.jaxrs.model.LogRecord.Object.LOAN;
import static org.folio.rest.jaxrs.model.LogRecord.Object.N_A;
import static org.folio.rest.jaxrs.model.LogRecord.Object.REQUEST;
import static org.folio.util.LogEventPayloadField.LOG_EVENT_TYPE;
import static org.folio.utils.TenantApiTestUtil.REQUEST_CREATED_THROUGH_OVERRIDE_PAYLOAD_JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.folio.utils.TenantApiTestUtil.CHECK_IN_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.getFile;

import org.folio.rest.jaxrs.model.LogRecord;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

public class AuditHandlersImplApiTest extends ApiTestBase {

  private final Logger logger = LogManager.getLogger();

  String EVENT_HANDLER_ENDPOINT = "/audit/handlers/log-record";

  @Test
  void postLogRecordEvent() {
    logger.info("post valid log event: success");

    // get initial number of records
    int initialNumberOfNaRecords = getNumberOfExistingLogRecords(N_A);
    int initialNumberOfLoanRecords = getNumberOfExistingLogRecords(LOAN);
    int initialNumberOfRequestRecords = getNumberOfExistingLogRecords(REQUEST);

    String payload = getFile(CHECK_IN_PAYLOAD_JSON);
    postLogRecord(payload);

    // check number of created records
    verifyNumberOfLogRecords(N_A, ++initialNumberOfNaRecords);
    verifyNumberOfLogRecords(LOAN, ++initialNumberOfLoanRecords);
    verifyNumberOfLogRecords(REQUEST, ++initialNumberOfRequestRecords);
  }

  @Test
  void postLogRecordEventForRequestOverride() {
    logger.info("post valid log event for request creation through override: success");

    int initialNumberOfRequestRecords = getNumberOfExistingLogRecords(REQUEST);
    postLogRecord(getFile(REQUEST_CREATED_THROUGH_OVERRIDE_PAYLOAD_JSON));
    verifyNumberOfLogRecords(REQUEST, ++initialNumberOfRequestRecords);
  }

  @Test
  void postLogRecordEventWithIllegalEventType() {
    logger.info("post valid log event: illegal event type");

    JsonObject payload = new JsonObject();
    payload.put(LOG_EVENT_TYPE.value(), "Illegal");

    postLogRecord(payload);
  }

  private void postLogRecord(JsonObject payload) {
    postLogRecord(payload.encode());
  }

  private void postLogRecord(String payload) {
    given().headers(HEADERS)
      .body(payload)
      .post(EVENT_HANDLER_ENDPOINT)
      .then()
      .log().all()
      .statusCode(204);
  }

  private void verifyNumberOfLogRecords(LogRecord.Object object, int expectedNumberOfRecords) {
    assertEquals(expectedNumberOfRecords, getNumberOfExistingLogRecords(object));
  }

  private int getNumberOfExistingLogRecords(LogRecord.Object object) {
    return given().headers(HEADERS)
      .get(CIRCULATION_LOGS_ENDPOINT + "?query=object=\"" + object.value() +"\"")
      .then()
      .log().all()
      .statusCode(200)
      .extract()
      .path("totalRecords");
  }
}
