package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.folio.rest.jaxrs.model.LogRecord.Object.LOAN;
import static org.folio.rest.jaxrs.model.LogRecord.Object.N_A;
import static org.folio.rest.jaxrs.model.LogRecord.Object.REQUEST;
import static org.folio.util.LogEventPayloadField.LOG_EVENT_TYPE;
import static org.hamcrest.Matchers.equalTo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

public class AuditHandlersImplApiTest extends ApiTestBase {

  private final Logger logger = LogManager.getLogger(AuditHandlersImplApiTest.class);

  String EVENT_HANDLER_ENDPOINT = "/audit/handlers/log-record";

  @Test
  void postLogRecordEvent() {
    logger.info("post valid log event: success");
    // check initial number of records
    verifyNumberOfLogRecords(Arrays.asList("Loan", "Request"), 1);

    String payload = getFile(CHECK_IN_PAYLOAD_JSON);

    given().headers(HEADERS)
      .body(payload)
      .post(EVENT_HANDLER_ENDPOINT)
      .then()
      .log().all()
      .statusCode(204);

    // check number of created records
    verifyNumberOfLogRecords(Collections.singletonList(N_A.value()), 1);
    verifyNumberOfLogRecords(Arrays.asList(LOAN.value(), REQUEST.value()), 2);
  }

  @Test
  void postLogRecordEventWithIllegalEventType() {
    logger.info("post valid log event: illegal event type");

    JsonObject payload = new JsonObject();
    payload.put(LOG_EVENT_TYPE.value(), "Illegal");

    given().headers(HEADERS)
      .body(payload.encode())
      .post(EVENT_HANDLER_ENDPOINT)
      .then()
      .log().all()
      .statusCode(204);
  }

  private void verifyNumberOfLogRecords(List<String> objects, int expectedNumberOfRecords) {
    for (String o : objects) {
      given().headers(HEADERS)
        .get(CIRCULATION_LOGS_ENDPOINT + "?query=object=\"" + o +"\"")
        .then()
        .log().all()
        .statusCode(200)
        .and()
        .body("totalRecords", equalTo(expectedNumberOfRecords));
    }
  }
}
