package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static java.util.stream.Collectors.groupingBy;
import static org.awaitility.Awaitility.await;
import static org.folio.rest.jaxrs.model.LogRecord.Action.CREATED_THROUGH_OVERRIDE;
import static org.folio.util.Constants.NO_BARCODE;
import static org.folio.utils.TenantApiTestUtil.LOAN_ANONYMIZE_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.SAMPLES;
import static org.folio.utils.TenantApiTestUtil.getFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.rest.jaxrs.model.LogRecord.Action;
import org.folio.rest.jaxrs.model.LogRecordCollection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CirculationLogsImplApiTest extends ApiTestBase {

  private final Logger logger = LogManager.getLogger();

  @BeforeAll
  public static void prepareSampleData() {

    SAMPLES.forEach(sample -> given().headers(HEADERS)
      .body(getFile(sample))
      .post("/audit/handlers/log-record")
      .then()
      .log().all()
      .statusCode(204));
  }

  @Test
  void getCirculationAuditLogRecordsNoFilter() {
    logger.info("Get circulation audit log records: no filter");
    given().headers(HEADERS).get(CIRCULATION_LOGS_ENDPOINT)
      .then().log().all().statusCode(200)
      .assertThat().body("totalRecords", equalTo(26));
  }

  @Test
  void getCirculationAuditLogRecordsFilterByAction() {
    logger.info("Get circulation audit log records: filter by action");
    LogRecordCollection records = given().headers(HEADERS).get(CIRCULATION_LOGS_ENDPOINT + "?query=action=Created")
      .then().log().all().statusCode(200).extract().body().as(LogRecordCollection.class);

    assertThat(records.getTotalRecords(), equalTo(3));
    assertThat(records.getLogRecords(), hasSize(3));

    Map<LogRecord.Object, List<LogRecord>> groupedByObjectRecords = records.getLogRecords().stream().collect(groupingBy(LogRecord::getObject));

    assertThat(groupedByObjectRecords.get(LogRecord.Object.MANUAL_BLOCK), hasSize(1));
    assertThat(groupedByObjectRecords.get(LogRecord.Object.MANUAL_BLOCK).get(0).getAction(), equalTo(Action.CREATED));

    assertThat(groupedByObjectRecords.get(LogRecord.Object.REQUEST), hasSize(2));
    Map<Action, List<LogRecord>> requestActions = groupedByObjectRecords.get(LogRecord.Object.REQUEST).stream().collect(groupingBy(LogRecord::getAction));

    assertThat(requestActions.get(Action.CREATED), notNullValue());
    assertThat(requestActions.get(CREATED_THROUGH_OVERRIDE), notNullValue());
  }

  @Test
  void getCirculationAuditLogRecordsFilterByUserBarcodeAndItemBarcode() {
    logger.info("Get circulation audit log records: filter by userBarcode and itemBarcode");
    given().headers(HEADERS).get(CIRCULATION_LOGS_ENDPOINT + "?query=(userBarcode=40187925817754 AND items=326547658598)")
      .then().log().all().statusCode(200)
      .assertThat()
      .body("logRecords[0].object", anyOf(equalTo("Loan"), equalTo("Request")))
      .and().body("logRecords[1].object", anyOf(equalTo("Loan"), equalTo("Request")))
      .and()
      .body("totalRecords", equalTo(2));
  }

  @Test
  void anonymizeLoanShouldRemoveUserDataFromRelatedRecords() {
    logger.info("Anonymize loan: user data from related records should be removed");

    given().headers(HEADERS)
      .body(getFile(LOAN_ANONYMIZE_PAYLOAD_JSON))
      .post("/audit/handlers/log-record")
      .then()
      .log().all()
      .statusCode(204);

    await().pollDelay(1, TimeUnit.SECONDS).until(() -> true);

    given().headers(HEADERS).get(CIRCULATION_LOGS_ENDPOINT + "?query=(items=845687423)")
      .then().log().all().statusCode(200)
      .assertThat()
      .body("totalRecords", equalTo(6))
      .body("logRecords[0].userBarcode", is(NO_BARCODE))
      .and().body("logRecords[0].linkToIds.userId", is(emptyOrNullString()))
      .and().body("logRecords[1].userBarcode", is(NO_BARCODE))
      .and().body("logRecords[1].linkToIds.userId", is(emptyOrNullString()))
      .and().body("logRecords[2].userBarcode", is(NO_BARCODE))
      .and().body("logRecords[2].linkToIds.userId", is(emptyOrNullString()))
      .and().body("logRecords[3].userBarcode", is(NO_BARCODE))
      .and().body("logRecords[3].linkToIds.userId", is(emptyOrNullString()))
      .and().body("logRecords[4].userBarcode", is(NO_BARCODE))
      .and().body("logRecords[4].linkToIds.userId", is(emptyOrNullString()))
      .and().body("logRecords[5].userBarcode", is(NO_BARCODE))
      .and().body("logRecords[5].linkToIds.userId", is(emptyOrNullString()));
  }

  @Test
  void getFeeFineRelatedRecordOfVirtualItem() {
    // For virtual item, holdingsId and instanceId needs to be present, then only FE validation will work.
    // This record is already posted in beforeAll method so directly assert it using get endpoint with virtual item ID.
    given().headers(HEADERS).get(CIRCULATION_LOGS_ENDPOINT + "?query=(items=100d10bf-2f06-4aa0-be15-0b95b2d9f9e4)")
      .then().log().all().statusCode(200)
      .assertThat()
      .body("totalRecords", equalTo(1))
      .body("logRecords[0].items[0].itemBarcode", is("virtualItem"))
      .and().body("logRecords[0].items[0].itemId", is("100d10bf-2f06-4aa0-be15-0b95b2d9f9e4"))
      .and().body("logRecords[0].items[0].instanceId", is("5bf370e0-8cca-4d9c-82e4-5170ab2a0a39"))
      .and().body("logRecords[0].items[0].holdingId", is("e3ff6133-b9a2-4d4c-a1c9-dc1867d4df19"));
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
