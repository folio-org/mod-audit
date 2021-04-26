package org.folio.builder.service;

import static org.folio.util.Constants.SYSTEM;
import static org.folio.utils.TenantApiTestUtil.REQUEST_CANCELLED_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.REQUEST_CREATED_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.REQUEST_EDITED_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.REQUEST_EXPIRED_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.REQUEST_MOVED_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.REQUEST_PICKUP_EXPIRED_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.REQUEST_REORDERED_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.getFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.LogRecord;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.vertx.core.json.JsonObject;


public class RequestRecordBuilderTest extends BuilderTestBase {

  private static final Logger logger = LogManager.getLogger();

  private static final String EXPECTED_CREATE_DESCRIPTION = "Type: Recall.";
  private static final String EXPECTED_EDITED_DESCRIPTION = "Type: Recall. New expiration date: 2020-10-23 00:00:00 (from: 2020-10-19 00:00:00). New fulfilment preference: Hold Shelf (from: Hold).";
  private static final String EXPECTED_MOVED_DESCRIPTION = "Type: Hold. New item barcode: 645398607547 (from: 653285216743).";
  private static final String EXPECTED_CANCELLED_DESCRIPTION = "Type: Hold. Reason for cancellation: Cancelled at patron’s request.";
  private static final String EXPECTED_REORDERED_DESCRIPTION = "Type: Recall. New queue position: 2 (from: 3).";
  private static final String EXPECTED_EXPIRED_DESCRIPTION = "Type: Hold. New request status: Closed - Unfilled (from: Open - Awaiting pickup).";
  private static final String EXPECTED_PICKUP_EXPIRED_DESCRIPTION = "Type: Hold. New request status: Closed - Pickup expired (from: Open - Awaiting pickup).";

  private enum TestValue {

    CREATED(LogRecord.Action.CREATED, REQUEST_CREATED_PAYLOAD_JSON, EXPECTED_CREATE_DESCRIPTION),
    EDITED(LogRecord.Action.EDITED, REQUEST_EDITED_PAYLOAD_JSON, EXPECTED_EDITED_DESCRIPTION),
    MOVED(LogRecord.Action.MOVED, REQUEST_MOVED_PAYLOAD_JSON, EXPECTED_MOVED_DESCRIPTION),
    CANCELLED(LogRecord.Action.CANCELLED, REQUEST_CANCELLED_PAYLOAD_JSON, EXPECTED_CANCELLED_DESCRIPTION),
    REORDERED(LogRecord.Action.QUEUE_POSITION_REORDERED, REQUEST_REORDERED_PAYLOAD_JSON, EXPECTED_REORDERED_DESCRIPTION),
    EXPIRED(LogRecord.Action.EXPIRED, REQUEST_EXPIRED_PAYLOAD_JSON, EXPECTED_EXPIRED_DESCRIPTION),
    PICKUP_EXPIRED(LogRecord.Action.PICKUP_EXPIRED, REQUEST_PICKUP_EXPIRED_PAYLOAD_JSON, EXPECTED_PICKUP_EXPIRED_DESCRIPTION);

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

    assertThat(requestLogRecord.getItems().get(0).getItemBarcode(), notNullValue());
    assertThat(requestLogRecord.getItems().get(0).getHoldingId(), notNullValue());
    assertThat(requestLogRecord.getItems().get(0).getInstanceId(), notNullValue());

    assertThat(requestLogRecord.getUserBarcode(), notNullValue());
    assertThat(requestLogRecord.getLinkToIds().getUserId(), notNullValue());
    assertThat(requestLogRecord.getLinkToIds().getRequestId(), notNullValue());

    if (TestValue.EXPIRED == value || TestValue.PICKUP_EXPIRED == value) {
      assertThat(requestLogRecord.getSource(), equalTo(SYSTEM));
    } else {
      assertThat(requestLogRecord.getSource(), equalTo("ADMINISTRATOR, DIKU"));
    }

    assertThat(requestLogRecord.getDescription(), equalTo(value.getDescription()));
  }
}
