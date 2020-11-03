package org.folio.builder.service;

import static org.folio.util.JsonPropertyFetcher.getNestedStringProperty;
import static org.folio.util.LogEventPayloadField.PAYLOAD;
import static org.folio.util.LogEventPayloadField.USER_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;

import org.folio.rest.jaxrs.model.LogRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ManualBlockRecordBuilderTest extends BuilderTestBase {

  private static final Logger logger = LoggerFactory.getLogger(ManualBlockRecordBuilderTest.class);

  private static final String EXPECTED_CREATE_DESCRIPTION = "Block actions: borrowing, renewals, requests. Description: Manual Block Description. Staff only information: Staff information. Message to patron: Message To Patron. Expiration date: 2018-10-23 00:00:00.";
  private static final String EXPECTED_UPDATE_DESCRIPTION = "Description: Manual Block Description. Staff only information: Staff information. Message to patron: Message To Patron. Expiration date: 2018-10-23 00:00:00.";
  private static final String EXPECTED_DELETE_DESCRIPTION = "Block actions: borrowing, requests. Description: Manual Block Description. Staff only information: Staff information. Message to patron: Message To Patron. Expiration date: 2018-10-23 00:00:00.";

  public static final String USER_NOT_FOUND_ID = "4a52f480-8f9a-49c8-9dbb-65f086e577fb";
  public static final String EXPECTED_SOURCE = "Denesik, Toney";

  private enum TestValue {

    CREATED(LogRecord.Action.CREATED, MANUAL_BLOCK_CREATED_PAYLOAD_JSON, EXPECTED_CREATE_DESCRIPTION),
    MODIFIED(LogRecord.Action.MODIFIED, MANUAL_BLOCK_UPDATED_PAYLOAD_JSON, EXPECTED_UPDATE_DESCRIPTION),
    DELETED(LogRecord.Action.DELETED, MANUAL_BLOCK_DELETED_PAYLOAD_JSON, EXPECTED_DELETE_DESCRIPTION);

    TestValue(LogRecord.Action action, String pathToPayload, String description) {
      this.action = action;
      this.pathToPayload = pathToPayload;
      this.description = description;
    }

    private final LogRecord.Action action;
    private final String pathToPayload;
    private final String description;

    public LogRecord.Action getAction() {
      return action;
    }

    public String getPathToPayload() {
      return pathToPayload;
    }

    public String getDescription() {
      return description;
    }
  }

  @ParameterizedTest
  @EnumSource(value = TestValue.class)
  void manualBlockLogRecordTest(TestValue value) throws Exception {
    logger.info("===== Test manual block log records builder: " + value + " =====");

    JsonObject payload = new JsonObject(getFile(value.getPathToPayload()));

    List<LogRecord> records = manualBlockRecordBuilder.buildLogRecord(payload).get();
    assertThat(records, hasSize(1));

    LogRecord manualBlockLogRecord = records.get(0);
    assertManualBlockLogRecord(value, payload, manualBlockLogRecord);
    assertThat(manualBlockLogRecord.getUserBarcode(), equalTo("693787594998493"));

    assertSource(value, manualBlockLogRecord);
  }

  @Test
  void manualBlockLogRecordWithoutUserTest() throws Exception {
    logger.info("===== Test manual block log records builder: user barcode isn't available =====");

    JsonObject payload = new JsonObject(getFile(TestValue.MODIFIED.getPathToPayload()));

    payload.getJsonObject(PAYLOAD.value()).put(USER_ID.value(), USER_NOT_FOUND_ID);

    List<LogRecord> records = manualBlockRecordBuilder.buildLogRecord(payload).get();
    assertThat(records, hasSize(1));

    LogRecord manualBlockLogRecord = records.get(0);
    assertManualBlockLogRecord(TestValue.MODIFIED, payload, manualBlockLogRecord);
    assertThat(manualBlockLogRecord.getUserBarcode(), nullValue());
    assertThat(manualBlockLogRecord.getSource(), nullValue());
  }

  private void assertManualBlockLogRecord(TestValue value, JsonObject payload, LogRecord manualBlockCreatedRecord) {
    assertThat(manualBlockCreatedRecord.getObject(), equalTo(LogRecord.Object.MANUAL_BLOCK));
    assertThat(manualBlockCreatedRecord.getAction(), equalTo(value.getAction()));
    assertThat(manualBlockCreatedRecord.getLinkToIds()
      .getUserId(), equalTo(getNestedStringProperty(payload, PAYLOAD, USER_ID)));
    assertThat(manualBlockCreatedRecord.getDescription(), equalTo(value.getDescription()));
  }

  private void assertSource(TestValue value, LogRecord manualBlockLogRecord) {
    if (value == TestValue.DELETED) {
      assertThat(manualBlockLogRecord.getSource(), nullValue());
    } else {
      assertThat(manualBlockLogRecord.getSource(), equalTo(EXPECTED_SOURCE));
    }
  }
}
