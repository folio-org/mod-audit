package org.folio.builder.record;

import static org.folio.util.JsonPropertyFetcher.getNestedStringProperty;
import static org.folio.util.LogEventPayloadField.PAYLOAD;
import static org.folio.util.LogEventPayloadField.USER_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;

import org.folio.rest.jaxrs.model.LogRecord;
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
  public void manualBlockLogRecordTest(TestValue value) {
    logger.info("===== Test manual block log records builder: " + value + " =====");

    JsonObject payload = new JsonObject(getFile(value.getPathToPayload()));

    List<LogRecord> records = manualBlockRecordBuilder.buildLogRecord(payload);
    assertThat(records, hasSize(1));

    LogRecord manualBlockCreatedRecord = records.get(0);
    assertThat(manualBlockCreatedRecord.getObject(), equalTo(LogRecord.Object.MANUAL_BLOCK));
    assertThat(manualBlockCreatedRecord.getAction(), equalTo(value.getAction()));
    assertThat(manualBlockCreatedRecord.getLinkToIds()
      .getUserId(), equalTo(getNestedStringProperty(payload, PAYLOAD, USER_ID)));
    assertThat(manualBlockCreatedRecord.getDescription(), equalTo(value.getDescription()));
  }
}
