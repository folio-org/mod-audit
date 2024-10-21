package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.folio.rest.jaxrs.model.LogRecord.Object.LOAN;
import static org.folio.rest.jaxrs.model.LogRecord.Object.NOTICE;
import static org.folio.rest.jaxrs.model.LogRecord.Object.N_A;
import static org.folio.rest.jaxrs.model.LogRecord.Object.REQUEST;
import static org.folio.util.LogEventPayloadField.LOG_EVENT_TYPE;
import static org.folio.utils.TenantApiTestUtil.CHECK_OUT_THROUGH_OVERRIDE_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.NOTICE_ERROR_FULL_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.REQUEST_CREATED_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.REQUEST_CREATED_THROUGH_OVERRIDE_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.REQUEST_EDITED_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.REQUEST_EDITED_PAYLOAD_WITH_NON_EMPTY_DATE_JSON;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.folio.utils.TenantApiTestUtil.CHECK_IN_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.getFile;

import com.jayway.jsonpath.JsonPath;
import org.folio.rest.jaxrs.model.LogRecord;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

public class AuditHandlersImplApiTest extends ApiTestBase {

  private final Logger logger = LogManager.getLogger();

  String EVENT_HANDLER_ENDPOINT = "/audit/handlers/log-record";
  public static final String FAILED_USER_ID = "1d0581d5-7f44-4cfb-b866-5f6f1d849f6d";

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
  void postLogRecordForCheckOutThroughOverrideEvent() {
    logger.info("post valid log event: success");

    // get initial number of records
    int initialNumberOfLoanRecords = getNumberOfExistingLogRecords(LOAN);
    int initialNumberOfRequestRecords = getNumberOfExistingLogRecords(REQUEST);

    String payload = getFile(CHECK_OUT_THROUGH_OVERRIDE_PAYLOAD_JSON);
    postLogRecord(payload);

    // check number of created records
    verifyNumberOfLogRecords(LOAN, ++initialNumberOfLoanRecords);
    verifyNumberOfLogRecords(REQUEST, ++initialNumberOfRequestRecords);
  }

  @ParameterizedTest
  @ValueSource(strings = {REQUEST_CREATED_THROUGH_OVERRIDE_PAYLOAD_JSON, REQUEST_EDITED_PAYLOAD_JSON, REQUEST_EDITED_PAYLOAD_WITH_NON_EMPTY_DATE_JSON})
  void postLogRecordEventForRequestOverride(String sample) {
    logger.info("post valid log event for request creation through override: success");

    int initialNumberOfRequestRecords = getNumberOfExistingLogRecords(REQUEST);
    postLogRecord(getFile(sample));
    verifyNumberOfLogRecords(REQUEST, ++initialNumberOfRequestRecords);
  }

  //@Test
  void postLogRecordEventForNoticeError() {
    logger.info("post valid log event for notice error: success");

    int initialNumberOfRequestRecords = getNumberOfExistingLogRecords(NOTICE);
    postLogRecord(getFile(NOTICE_ERROR_FULL_PAYLOAD_JSON));
    verifyNumberOfLogRecords(NOTICE, ++initialNumberOfRequestRecords);
  }

  @Test
  void postLogRecordEventForUserRetrieveError() {
    logger.info("post valid log event for request creation : fail due to user retrieving error");

    var dc = JsonPath.parse(getFile(REQUEST_CREATED_PAYLOAD_JSON))
      .set("$.payload.requests.created.requesterId", FAILED_USER_ID);

    int initialNumberOfRequestRecords = getNumberOfExistingLogRecords(REQUEST);
    postLogRecord(dc.jsonString());

    // Record isn't created without details of user
    verifyNumberOfLogRecords(REQUEST, initialNumberOfRequestRecords);
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
    await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
      assertEquals(expectedNumberOfRecords, getNumberOfExistingLogRecords(object)));
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
