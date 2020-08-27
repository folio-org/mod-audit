package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.http.Headers;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
public class CirculationLogsImplTest extends TestBase {
  private final Logger logger = LoggerFactory.getLogger(CirculationLogsImplTest.class);
  private final String CIRC_LOGS_ENDPOINT = "/audit-data/circulation/logs";
  private Headers headers = new Headers(tenant, perms, ctype);

  @Test
  @Order(1)
  void initializeDatabaseAndLoadSampleData() {
    logger.info("Initialize database and load sample data");
    String tenants = "{\"module_to\":\"" + moduleId + "\"," +
      "\"parameters\": [ { \"key\":\"loadSample\", \"value\": true } ] }";
    given().headers(headers).body(tenants).post("/_/tenant").then().log().all()
      .statusCode(201);
  }

  @Test
  @Order(2)
  void getCirculationAuditLogRecordsNoFilter() {
    logger.info("Get circulation audit log records: no filter");
    given().headers(headers).get(CIRC_LOGS_ENDPOINT)
      .then().log().all().statusCode(200)
      .assertThat().body("totalRecords", equalTo(7));
  }

  @Test
  @Order(3)
  void getCirculationAuditLogRecordsFilterByAction() {
    logger.info("Get circulation audit log records: filter by action");
    given().headers(headers).get(CIRC_LOGS_ENDPOINT + "?query=action=Created")
      .then().log().all().statusCode(200)
      .assertThat().body("logRecords[0].object", equalTo("Item Block"))
      .and().body("logRecords[1].object", equalTo("Request"))
      .and().body("totalRecords", equalTo(2));
  }

  @Test
  @Order(4)
  void getCirculationAuditLogRecordsFilterByUserBarcodeAndItemBarcode() {
    logger.info("Get circulation audit log records: filter by userBarcode and itemBarcode");
    given().headers(headers).get(CIRC_LOGS_ENDPOINT + "?query=(userBarcode=1000024158 AND itemBarcode=12983765)")
      .then().log().all().statusCode(200)
      .assertThat().body("logRecords[0].object", equalTo("Fee/Fine"))
      .and().body("logRecords[1].object", equalTo("Item Block"))
      .and().body("totalRecords", equalTo(2));
  }

  @Test
  @Order(5)
  void getCirculationAuditLogRecordsMalformedQuery() {
    logger.info("get circulation audit log records: malformed query");
    given().headers(headers).get(CIRC_LOGS_ENDPOINT + "?query=userbarcod=1000024158")
      .then().log().all().statusCode(200)
      .and().body("totalRecords", equalTo(0));
  }

  @Test
  @Order(6)
  void getCirculationAuditLogRecordsInvalidQuery() {
    logger.info("get circulation audit log records: invalid query");
    given().headers(headers).get(CIRC_LOGS_ENDPOINT + "?query=abcd")
      .then().log().all().statusCode(422);
  }

  @Test
  @Order(7)
  void deleteTenant() {
    logger.info("Delete tenant");
    given().headers(headers).delete("/_/tenant").then().log().all().statusCode(204);
  }
}
