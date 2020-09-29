package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.junit.jupiter.api.Test;

class CirculationLogsImplTest extends TestBase {

  private final Logger logger = LoggerFactory.getLogger(CirculationLogsImplTest.class);

  @Test
  void getCirculationAuditLogRecordsNoFilter() {
    logger.info("Get circulation audit log records: no filter");
    given().headers(headers).get(CIRC_LOGS_ENDPOINT)
      .then().log().all().statusCode(200)
      .assertThat().body("totalRecords", equalTo(7));
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
    given().headers(headers).get(CIRC_LOGS_ENDPOINT + "?query=(userBarcode=1000024158 AND items=12983765)")
      .then().log().all().statusCode(200)
      .assertThat().body("logRecords[0].object", anyOf(equalTo("Item Block"), equalTo("Fee/Fine")))
      .and().body("logRecords[1].object", anyOf(equalTo("Item Block"), equalTo("Fee/Fine")))
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
}
