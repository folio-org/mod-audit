package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static java.util.stream.Collectors.groupingBy;
import static org.folio.rest.jaxrs.model.LogRecord.Action.CREATED_THROUGH_OVERRIDE;
import static org.folio.utils.TenantApiTestUtil.REQUEST_CREATED_THROUGH_OVERRIDE_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.getFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.rest.jaxrs.model.LogRecord.Action;
import org.folio.rest.jaxrs.model.LogRecordCollection;
import org.junit.jupiter.api.Test;

public class CirculationLogsImplApiTest extends ApiTestBase {

  private final Logger logger = LogManager.getLogger();

  @Test
  void getCirculationAuditLogRecordsNoFilter() {
    logger.info("Get circulation audit log records: no filter");
    given().headers(HEADERS).get(CIRCULATION_LOGS_ENDPOINT)
      .then().log().all().statusCode(200)
      .assertThat().body("totalRecords", equalTo(8));
  }

  @Test
  void getCirculationAuditLogRecordsFilterByAction() {
    logger.info("Get circulation audit log records: filter by action");
    LogRecordCollection records = given().headers(HEADERS).get(CIRCULATION_LOGS_ENDPOINT + "?query=action=Created")
      .then().log().all().statusCode(200).extract().body().as(LogRecordCollection.class);

    assertThat(records.getTotalRecords(), equalTo(3));
    assertThat(records.getLogRecords(), hasSize(3));

    Map<LogRecord.Object, List<LogRecord>> groupedByObjectRecords = records.getLogRecords().stream().collect(groupingBy(LogRecord::getObject));

    assertThat(groupedByObjectRecords.get(LogRecord.Object.ITEM_BLOCK), hasSize(1));
    assertThat(groupedByObjectRecords.get(LogRecord.Object.ITEM_BLOCK).get(0).getAction(), equalTo(Action.CREATED));

    assertThat(groupedByObjectRecords.get(LogRecord.Object.REQUEST), hasSize(2));
    assertThat(groupedByObjectRecords.get(LogRecord.Object.REQUEST).get(0).getAction(), equalTo(Action.CREATED));

  }

  @Test
  void getCirculationAuditLogRecordsFilterByUserBarcodeAndItemBarcode() {
    logger.info("Get circulation audit log records: filter by userBarcode and itemBarcode");
    given().headers(HEADERS).get(CIRCULATION_LOGS_ENDPOINT + "?query=(userBarcode=1000024158 AND items=12983765)")
      .then().log().all().statusCode(200)
      .assertThat().body("logRecords[0].object", anyOf(equalTo("Item Block"), equalTo("Fee/Fine")))
      .and().body("logRecords[1].object", anyOf(equalTo("Item Block"), equalTo("Fee/Fine")))
      .and().body("totalRecords", equalTo(2));
  }

  @Test
  void getCirculationAuditLogRecordsMalformedQuery() {
    logger.info("get circulation audit log records: malformed query");
    given().headers(HEADERS).get(CIRCULATION_LOGS_ENDPOINT + "?query=userbarcod=1000024158")
      .then().log().all().statusCode(200)
      .and().body("totalRecords", equalTo(0));
  }

  @Test
  void getCirculationAuditLogRecordsInvalidQuery() {
    logger.info("get circulation audit log records: invalid query");
    given().headers(HEADERS).get(CIRCULATION_LOGS_ENDPOINT + "?query=abcd")
      .then().log().all().statusCode(400);
  }

  @Test
  void getCirculationAuditLogRecordsForRequestCreatedThroughOverride() {
    verifyNumberOfLogRecordsWithAction(CREATED_THROUGH_OVERRIDE, 0);

    given().headers(HEADERS)
      .body(getFile(REQUEST_CREATED_THROUGH_OVERRIDE_PAYLOAD_JSON))
      .post("/audit/handlers/log-record")
      .then()
      .log().all()
      .statusCode(204);

    verifyNumberOfLogRecordsWithAction(CREATED_THROUGH_OVERRIDE, 1);
  }

  private void verifyNumberOfLogRecordsWithAction(Action action, int expectedNumberOfRecords) {
    String query = String.format("action=\"%s\"", action.value());
    verifyNumberOfLogRecords(query, expectedNumberOfRecords);
  }

  private void verifyNumberOfLogRecords(String query, int expectedNumberOfRecords) {
    String url = String.format("%s?query=%s", CIRCULATION_LOGS_ENDPOINT, query);

    given().headers(HEADERS)
      .get(url)
      .then()
      .log().all()
      .statusCode(200)
      .assertThat().body("totalRecords", equalTo(expectedNumberOfRecords));
  }
}
