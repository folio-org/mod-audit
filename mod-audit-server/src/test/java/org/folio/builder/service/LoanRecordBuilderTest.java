package org.folio.builder.service;

import static org.folio.rest.jaxrs.model.LogRecord.Action.AGE_TO_LOST;
import static org.folio.rest.jaxrs.model.LogRecord.Action.ANONYMIZE;
import static org.folio.rest.jaxrs.model.LogRecord.Action.CHANGED_DUE_DATE;
import static org.folio.rest.jaxrs.model.LogRecord.Action.RENEWED;
import static org.folio.util.Constants.SYSTEM;
import static org.folio.util.LogEventPayloadField.ACTION;
import static org.folio.utils.TenantApiTestUtil.LOAN_AGE_TO_LOST_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.LOAN_ANONYMIZE_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.LOAN_CHANGED_DUE_DATE_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.LOAN_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.LOAN_WRONG_ACTION_JSON;
import static org.folio.utils.TenantApiTestUtil.LOAN_EMPTY_ACTION_JSON;
import static org.folio.utils.TenantApiTestUtil.getFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import io.vertx.core.json.Json;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.LogRecord;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

public class LoanRecordBuilderTest extends BuilderTestBase {
  private static final Logger logger = LogManager.getLogger();

  @Test
  void testLoan() throws Exception {
    logger.info("===== Test loan log records builder =====");

    JsonObject payload = new JsonObject(getFile(LOAN_PAYLOAD_JSON));
    List<LogRecord> records = loanRecordBuilder.buildLogRecord(payload).get();

    assertThat(records.size(), equalTo(1));

    LogRecord loanLogRecord = records.get(0);
    assertThat(loanLogRecord.getUserBarcode(), equalTo("631888472578232"));
    assertThat(loanLogRecord.getItems().size(), equalTo(1));
    assertThat(loanLogRecord.getItems().get(0).getItemBarcode(), equalTo("90000"));
    assertThat(loanLogRecord.getItems().get(0).getItemId(), equalTo("100d10bf-2f06-4aa0-be15-0b95b2d9f9e3"));
    assertThat(loanLogRecord.getItems().get(0).getInstanceId(), equalTo("5bf370e0-8cca-4d9c-82e4-5170ab2a0a39"));
    assertThat(loanLogRecord.getItems().get(0).getHoldingId(), equalTo("e3ff6133-b9a2-4d4c-a1c9-dc1867d4df19"));
    assertThat(loanLogRecord.getItems().get(0).getLoanId(), equalTo("336ec84c-27ed-483d-92e3-926fafa7ed1c"));
    assertThat(loanLogRecord.getObject(), equalTo(LogRecord.Object.LOAN));
    assertThat(loanLogRecord.getAction(), equalTo(RENEWED));
    assertThat(loanLogRecord.getDate(), is(not(nullValue())));
    assertThat(loanLogRecord.getServicePointId(), equalTo("c4c90014-c8c9-4ade-8f24-b5e313319f4b"));
    assertThat(loanLogRecord.getSource(), equalTo("ADMINISTRATOR, DIKU"));
    assertThat(loanLogRecord.getDescription(),
      equalTo("New due date: 2020-10-14T11:10:41.554Z (from 2020-10-14T11:03:00.751Z)"));
  }

  @Test
  void testLoanAnonymize() throws Exception {
    logger.info("===== Test loan log records builder: Anonymize loan =====");

    JsonObject payload = new JsonObject(getFile(LOAN_ANONYMIZE_PAYLOAD_JSON));
    List<LogRecord> records = loanRecordBuilder.buildLogRecord(payload).get();

    assertThat(records.size(), equalTo(1));

    LogRecord loanLogRecord = records.get(0);
    assertThat(loanLogRecord.getItems().size(), equalTo(1));
    assertThat(loanLogRecord.getItems().get(0).getItemBarcode(), equalTo("845687423"));
    assertThat(loanLogRecord.getItems().get(0).getItemId(), equalTo("e038e283-4104-455d-80b7-e6d05ca4e5a7"));
    assertThat(loanLogRecord.getItems().get(0).getInstanceId(), equalTo("5bf370e0-8cca-4d9c-82e4-5170ab2a0a39"));
    assertThat(loanLogRecord.getItems().get(0).getHoldingId(), equalTo("e3ff6133-b9a2-4d4c-a1c9-dc1867d4df19"));
    assertThat(loanLogRecord.getItems().get(0).getLoanId(), equalTo("f93c3359-682c-4c87-aed5-2d3e9357d41d"));
    assertThat(loanLogRecord.getObject(), equalTo(LogRecord.Object.LOAN));
    assertThat(loanLogRecord.getAction(), equalTo(ANONYMIZE));
    assertThat(loanLogRecord.getDate(), is(not(nullValue())));
    assertThat(loanLogRecord.getServicePointId(), equalTo("c4c90014-c8c9-4ade-8f24-b5e313319f4b"));
    assertThat(loanLogRecord.getSource(), equalTo(SYSTEM));
    assertThat(loanLogRecord.getUserBarcode(), isEmptyOrNullString());
    assertThat(loanLogRecord.getLinkToIds().getUserId(), isEmptyOrNullString());
  }

  @Test
  void testAgeToLost() throws Exception {
    logger.info("===== Test loan log records builder: Aged to lost loan =====");

    JsonObject payload = new JsonObject(getFile(LOAN_AGE_TO_LOST_PAYLOAD_JSON));
    List<LogRecord> records = loanRecordBuilder.buildLogRecord(payload).get();

    assertThat(records.size(), equalTo(1));

    LogRecord loanLogRecord = records.get(0);
    assertThat(loanLogRecord.getItems().size(), equalTo(1));
    assertThat(loanLogRecord.getUserBarcode(), equalTo("631888472578232"));
    assertThat(loanLogRecord.getItems().get(0).getItemBarcode(), equalTo("90000"));
    assertThat(loanLogRecord.getItems().get(0).getItemId(), equalTo("100d10bf-2f06-4aa0-be15-0b95b2d9f9e3"));
    assertThat(loanLogRecord.getItems().get(0).getInstanceId(), equalTo("5bf370e0-8cca-4d9c-82e4-5170ab2a0a39"));
    assertThat(loanLogRecord.getItems().get(0).getHoldingId(), equalTo("e3ff6133-b9a2-4d4c-a1c9-dc1867d4df19"));
    assertThat(loanLogRecord.getItems().get(0).getLoanId(), equalTo("336ec84c-27ed-483d-92e3-926fafa7ed1c"));
    assertThat(loanLogRecord.getObject(), equalTo(LogRecord.Object.LOAN));
    assertThat(loanLogRecord.getAction(), equalTo(AGE_TO_LOST));
    assertThat(loanLogRecord.getDate(), is(not(nullValue())));
    assertThat(loanLogRecord.getServicePointId(), nullValue());
    assertThat(loanLogRecord.getSource(), equalTo(SYSTEM));
  }

  @Test
  void testChangedDueDate() throws Exception {
    logger.info("===== Test loan log records builder: Changed due date loan =====");

    JsonObject payload = new JsonObject(getFile(LOAN_CHANGED_DUE_DATE_PAYLOAD_JSON));
    List<LogRecord> records = loanRecordBuilder.buildLogRecord(payload).get();

    assertThat(records.size(), equalTo(1));

    LogRecord loanLogRecord = records.get(0);
    assertThat(loanLogRecord.getItems().size(), equalTo(1));
    assertThat(loanLogRecord.getUserBarcode(), equalTo("631888472578232"));
    assertThat(loanLogRecord.getItems().get(0).getItemBarcode(), equalTo("90000"));
    assertThat(loanLogRecord.getItems().get(0).getItemId(), equalTo("100d10bf-2f06-4aa0-be15-0b95b2d9f9e3"));
    assertThat(loanLogRecord.getItems().get(0).getInstanceId(), equalTo("5bf370e0-8cca-4d9c-82e4-5170ab2a0a39"));
    assertThat(loanLogRecord.getItems().get(0).getHoldingId(), equalTo("e3ff6133-b9a2-4d4c-a1c9-dc1867d4df19"));
    assertThat(loanLogRecord.getItems().get(0).getLoanId(), equalTo("336ec84c-27ed-483d-92e3-926fafa7ed1c"));
    assertThat(loanLogRecord.getObject(), equalTo(LogRecord.Object.LOAN));
    assertThat(loanLogRecord.getAction(), equalTo(CHANGED_DUE_DATE));
    assertThat(loanLogRecord.getDate(), is(not(nullValue())));
    assertThat(loanLogRecord.getServicePointId(), equalTo("c4c90014-c8c9-4ade-8f24-b5e313319f4b"));
    assertThat(loanLogRecord.getSource(), equalTo("ADMINISTRATOR, DIKU"));
  }

  @Test
  void testLoanWithWrongAction() {
    logger.info("===== Test loan log records builder: An IllegalArgumentException will be thrown if action is wrong or null =====");

    JsonObject wrongAction = new JsonObject(getFile(LOAN_WRONG_ACTION_JSON));
    assertThrows(IllegalArgumentException.class, () -> loanRecordBuilder.buildLogRecord(wrongAction).get());

    JsonObject nullAction = wrongAction.getJsonObject("payload").putNull(ACTION.value());
    Exception thrown = assertThrows(IllegalArgumentException.class, () -> loanRecordBuilder.buildLogRecord(nullAction).get());
    assertEquals("Action is empty", thrown.getMessage());

    JsonObject emptyAction = new JsonObject(getFile(LOAN_EMPTY_ACTION_JSON));
    thrown = assertThrows(IllegalArgumentException.class, () -> loanRecordBuilder.buildLogRecord(emptyAction).get());
    assertEquals("Action is empty", thrown.getMessage());
  }
}
