package org.folio.builder.service;

import static org.folio.util.JsonPropertyFetcher.getNestedStringProperty;
import static org.folio.util.LogEventPayloadField.ITEM_BARCODE;
import static org.folio.util.LogEventPayloadField.PAYLOAD;
import static org.folio.util.LogEventPayloadField.USER_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;

import org.folio.rest.jaxrs.model.LogRecord;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class RequestRecordBuilderTest extends BuilderTestBase {

  private static final Logger logger = LoggerFactory.getLogger(ManualBlockRecordBuilderTest.class);

  private static final String EXPECTED_CREATE_DESCRIPTION = "Type: Recall.";
  private static final String EXPECTED_EDITED_DESCRIPTION = "Type: Recall. New expiration date: 2020-10-23 00:00:00 (from: 2020-10-19 00:00:00). New fulfilment preference: Hold Shelf (from: Hold).";
  private static final String EXPECTED_MOVED_DESCRIPTION = "Type: Hold. New item barcode: 645398607547 (from: 653285216743).";
  private static final String EXPECTED_CANCELLED_DESCRIPTION = "Type: Hold.";
  private static final String EXPECTED_REORDERED_DESCRIPTION = "Type: Recall. New queue position: 2 (from: 3).";

  private enum TestValue {

    CREATED(LogRecord.Action.CREATED, REQUEST_CREATED_PAYLOAD_JSON, EXPECTED_CREATE_DESCRIPTION),
    EDITED(LogRecord.Action.EDITED, REQUEST_EDITED_PAYLOAD_JSON, EXPECTED_EDITED_DESCRIPTION),
    MOVED(LogRecord.Action.MOVED, REQUEST_MOVED_PAYLOAD_JSON, EXPECTED_MOVED_DESCRIPTION),
    CANCELLED(LogRecord.Action.CANCELLED, REQUEST_CANCELLED_PAYLOAD_JSON, EXPECTED_CANCELLED_DESCRIPTION),
    REORDERED(LogRecord.Action.QUEUE_POSITION_REORDERED, REQUEST_REORDERED_PAYLOAD_JSON, EXPECTED_REORDERED_DESCRIPTION);

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
  void requestLogRecordTest(TestValue value) throws Exception {
    logger.info("===== Test requests log records builder: " + value + " =====");

    JsonObject payload = new JsonObject(getFile(value.getPathToPayload()));

    List<LogRecord> records = requestLogRecordBuilder.buildLogRecord(payload).get();
    assertThat(records, hasSize(1));

    LogRecord requestLogRecord = records.get(0);
    assertThat(requestLogRecord.getObject(), equalTo(LogRecord.Object.REQUEST));
    assertThat(requestLogRecord.getAction(), equalTo(value.getAction()));

    assertThat(requestLogRecord.getServicePointId(), notNullValue());

    assertThat(requestLogRecord.getItems().get(0).getItemBarcode(), notNullValue());
    assertThat(requestLogRecord.getItems().get(0).getItemBarcode(), equalTo(getNestedStringProperty(payload, PAYLOAD, ITEM_BARCODE)));

    assertThat(requestLogRecord.getLinkToIds().getUserId(), equalTo(getNestedStringProperty(payload, PAYLOAD, USER_ID)));

    assertThat(requestLogRecord.getDescription(), equalTo(value.getDescription()));
  }
}
