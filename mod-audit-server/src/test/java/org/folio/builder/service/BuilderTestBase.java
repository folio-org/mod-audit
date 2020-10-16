package org.folio.builder.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.TestSuite.isInitialized;
import static org.folio.TestSuite.wireMockServer;
import static org.folio.util.JsonPropertyFetcher.getArrayProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.HOLDINGS_RECORD_ID;
import static org.folio.util.LogEventPayloadField.INSTANCE_ID;
import static org.folio.util.LogEventPayloadField.ITEM_BARCODE;
import static org.folio.util.LogEventPayloadField.ITEM_ID;
import static org.folio.util.LogEventPayloadField.LOAN_ID;
import static org.folio.util.LogEventPayloadField.REQUESTS;
import static org.folio.util.LogEventPayloadField.REQUEST_ID;
import static org.folio.util.LogEventPayloadField.SERVICE_POINT_ID;
import static org.folio.util.LogEventPayloadField.SOURCE;
import static org.folio.util.LogEventPayloadField.USER_BARCODE;
import static org.folio.util.LogEventPayloadField.USER_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.folio.TestBase;
import org.folio.TestSuite;
import org.folio.rest.jaxrs.model.LogRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class BuilderTestBase extends TestBase {

  static LogRecordBuilderService checkInRecordBuilder, checkOutRecordBuilder, manualBlockRecordBuilder,
    feeFineRecordBuilder, noticeRecordBuilder, loanRecordBuilder;

  @BeforeAll
  public static void setUp() throws InterruptedException, ExecutionException, TimeoutException {
    if (!isInitialized) {
      TestSuite.globalInitialize();
    }

    wireMockServer.stubFor(get(urlEqualTo("/users/fa96fec8-786f-505b-b55e-e0bb1150d548"))
      .willReturn(aResponse().withBody(new JsonObject()
        .put("personal", new JsonObject().put("firstName", "John").put("lastName", "Johnson")).encode())
        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withStatus(200)));

    wireMockServer.stubFor(get(urlEqualTo("/templates/fa96fec8-786f-505b-b55e-e0bb1150d548"))
      .willReturn(aResponse().withBody(new JsonObject()
        .put("name", "sample template name").encode())
        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
        .withStatus(200)));

    checkInRecordBuilder = new CheckInRecordBuilderService(TestSuite.getVertx().getOrCreateContext(), headersMap);
    checkOutRecordBuilder = new CheckOutRecordBuilderService(TestSuite.getVertx().getOrCreateContext(), headersMap);
    manualBlockRecordBuilder = new ManualBlockRecordBuilderService(TestSuite.getVertx().getOrCreateContext(), headersMap);
    feeFineRecordBuilder = new FeeFineRecordBuilderService(TestSuite.getVertx().getOrCreateContext(), headersMap);
    noticeRecordBuilder = new NoticeRecordBuilderService(TestSuite.getVertx().getOrCreateContext(), headersMap);
    loanRecordBuilder = new LoanRecordBuilderService(TestSuite.getVertx().getOrCreateContext(), headersMap);
  }

  @AfterAll
  public static void tearDown() {
    if (isInitialized) {
      TestSuite.globalTearDown();
    }
  }

  public void validateRequestStatusChangedContent(JsonObject payload, LogRecord requestStatusChanged) {
    JsonArray requests = getArrayProperty(payload, REQUESTS);
    JsonObject request = requests.getJsonObject(0);
    assertThat(requestStatusChanged.getLinkToIds().getRequestId(), equalTo(getProperty(request, REQUEST_ID)));
  }

  public void validateAdditionalContent(JsonObject payload, LogRecord itemCheckedOutRecord) {
    assertThat(itemCheckedOutRecord.getLinkToIds().getUserId(), equalTo(getProperty(payload, USER_ID)));
    assertThat(itemCheckedOutRecord.getItems().get(0).getItemId(), equalTo(getProperty(payload, ITEM_ID)));
    assertThat(itemCheckedOutRecord.getItems().get(0).getLoanId(), equalTo(getProperty(payload, LOAN_ID)));
  }

  public void validateBaseContent(JsonObject payload, LogRecord itemCheckedOutRecord) {
    assertThat(itemCheckedOutRecord.getUserBarcode(), equalTo(getProperty(payload, USER_BARCODE)));
    assertThat(itemCheckedOutRecord.getItems().get(0).getItemBarcode(), equalTo(getProperty(payload, ITEM_BARCODE)));
    assertThat(itemCheckedOutRecord.getItems().get(0).getHoldingId(), equalTo(getProperty(payload, HOLDINGS_RECORD_ID)));
    assertThat(itemCheckedOutRecord.getItems().get(0).getInstanceId(), equalTo(getProperty(payload, INSTANCE_ID)));
    assertThat(itemCheckedOutRecord.getSource(), equalTo(getProperty(payload, SOURCE)));
    assertThat(itemCheckedOutRecord.getServicePointId(), equalTo(getProperty(payload, SERVICE_POINT_ID)));
  }
}
