package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static java.util.stream.Collectors.groupingBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.rest.jaxrs.model.LogRecordCollection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class CirculationLogsImplApiTest extends ApiBaseTest {

  private final Logger logger = LoggerFactory.getLogger(CirculationLogsImplApiTest.class);

  @Test
  void getCirculationAuditLogRecordsNoFilter() {
    logger.info("Get circulation audit log records: no filter");
    given().headers(HEADERS).get(CIRCULATION_LOGS_ENDPOINT)
      .then().log().all().statusCode(200)
      .assertThat().body("totalRecords", equalTo(7));
  }

  @Test
  void getCirculationAuditLogRecordsFilterByAction() {
    logger.info("Get circulation audit log records: filter by action");
    LogRecordCollection records = given().headers(HEADERS).get(CIRCULATION_LOGS_ENDPOINT + "?query=action=Created")
      .then().log().all().statusCode(200).extract().body().as(LogRecordCollection.class);

    assertThat(records.getTotalRecords(), equalTo(2));
    assertThat(records.getLogRecords(), hasSize(2));

    Map<LogRecord.Object, List<LogRecord>> groupedByObjectRecords = records.getLogRecords().stream().collect(groupingBy(LogRecord::getObject));

    assertThat(groupedByObjectRecords.get(LogRecord.Object.ITEM_BLOCK), hasSize(1));
    assertThat(groupedByObjectRecords.get(LogRecord.Object.ITEM_BLOCK).get(0).getAction(), equalTo(LogRecord.Action.CREATED));

    assertThat(groupedByObjectRecords.get(LogRecord.Object.REQUEST), hasSize(1));
    assertThat(groupedByObjectRecords.get(LogRecord.Object.REQUEST).get(0).getAction(), equalTo(LogRecord.Action.CREATED));

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
}
