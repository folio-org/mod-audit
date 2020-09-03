package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.jaxrs.model.LogEventPayload;
import org.junit.jupiter.api.Test;

class CirculationLogsImplTest extends TestBase {
  private final Logger logger = LoggerFactory.getLogger(CirculationLogsImplTest.class);
  private final String CIRC_LOGS_ENDPOINT = "/audit-data/circulation/logs";
  private final String EVENT_HANDLER_ENDPOINT = "/audit-data/circulation/event/handler";

  @Test
  void getCirculationAuditLogRecordsNoFilter() {
    logger.info("Get circulation audit log records: no filter");
    given().headers(headers).get(CIRC_LOGS_ENDPOINT)
      .then().log().all().statusCode(200)
      .assertThat().body("totalRecords", equalTo(8));
  }

  @Test
  void getCirculationAuditLogRecordsFilterByAction() {
    logger.info("Get circulation audit log records: filter by action");
    given().headers(headers).get(CIRC_LOGS_ENDPOINT + "?query=action=Created")
      .then().log().all().statusCode(200)
      .assertThat().body("logRecords[0].object", equalTo("Item Block"))
      .and().body("logRecords[1].object", equalTo("Request"))
      .and().body("totalRecords", equalTo(2));
  }

  @Test
  void getCirculationAuditLogRecordsFilterByUserBarcodeAndItemBarcode() {
    logger.info("Get circulation audit log records: filter by userBarcode and itemBarcode");
    given().headers(headers).get(CIRC_LOGS_ENDPOINT + "?query=(userBarcode=1000024158 AND itemBarcode=12983765)")
      .then().log().all().statusCode(200)
      .assertThat().body("logRecords[0].object", equalTo("Fee/Fine"))
      .and().body("logRecords[1].object", equalTo("Item Block"))
      .and().body("totalRecords", equalTo(2));
  }

  @Test
  void getCirculationAuditLogRecordsMalformedQuery() {
    logger.info("get circulation audit log records: malformed query");
    given().headers(headers).get(CIRC_LOGS_ENDPOINT + "?query=userbarcod=1000024158")
      .then().log().all().statusCode(200)
      .and().body("totalRecords", equalTo(0));
  }

  @Test
  void getCirculationAuditLogRecordsInvalidQuery() {
    logger.info("get circulation audit log records: invalid query");
    given().headers(headers).get(CIRC_LOGS_ENDPOINT + "?query=abcd")
      .then().log().all().statusCode(400);
  }

  @Test
  void postLogRecordEvent() {
    logger.info("post valid log event: success");
    // check initial number of Loans
    given().headers(headers).get(CIRC_LOGS_ENDPOINT + "?query=object=Loan")
      .then().log().all().statusCode(200)
      .and().body("totalRecords", equalTo(1));

    LogEventPayload payload = new LogEventPayload().withLoggedObjectType(LogEventPayload.LoggedObjectType.LOAN)
      .withRequest("{\"request\"}").withResponse("{\"response\"}");

    given().headers(headers).body(payload).post(EVENT_HANDLER_ENDPOINT)
      .then().log().all().statusCode(201);
    given().headers(headers).get(CIRC_LOGS_ENDPOINT + "?query=object=Loan")
      .then().log().all().statusCode(200)
      .and().body("totalRecords", equalTo(2));
  }
}
