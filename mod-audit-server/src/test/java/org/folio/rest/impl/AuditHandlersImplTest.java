package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.folio.rest.jaxrs.model.LogEventPayload;
import org.junit.jupiter.api.Test;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class AuditHandlersImplTest extends TestBase {

  private final Logger logger = LoggerFactory.getLogger(AuditHandlersImplTest.class);

  private final String EVENT_HANDLER_ENDPOINT = "/audit/handlers/log-record";

  @Test
  void postLogRecordEvent() {
    logger.info("post valid log event: success");
    // check initial number of Loans
    given().headers(headers).get(CIRC_LOGS_ENDPOINT + "?query=object=Loan")
      .then().log().all().statusCode(200)
      .and().body("totalRecords", equalTo(1));

    LogEventPayload payload = new LogEventPayload()
      .withLoggedObjectType(LogEventPayload.LoggedObjectType.LOAN)
      .withRequest("{\"request: a\"}")
      .withResponse("{\"response: b\"}");

    given().headers(headers).body(payload).post(EVENT_HANDLER_ENDPOINT)
      .then().log().all().statusCode(204);

    given().headers(headers).get(CIRC_LOGS_ENDPOINT + "?query=object=Loan")
      .then().log().all().statusCode(200)
      .and().body("totalRecords", equalTo(2));
  }

}
