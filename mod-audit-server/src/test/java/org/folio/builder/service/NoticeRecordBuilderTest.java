package org.folio.builder.service;

import static org.folio.rest.jaxrs.model.LogRecord.Action.SEND;
import static org.folio.rest.jaxrs.model.LogRecord.Action.SEND_ERROR;
import static org.folio.utils.TenantApiTestUtil.NOTICE_ERROR_FULL_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.NOTICE_ERROR_MINIMAL_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.NOTICE_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.getFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.LogRecord;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

public class NoticeRecordBuilderTest extends BuilderTestBase {
  private static final Logger logger = LogManager.getLogger();

  @Test
  void testNotice() throws Exception {
    logger.info("===== Test notice log records builder =====");

    JsonObject payload = new JsonObject(getFile(NOTICE_PAYLOAD_JSON));
    List<LogRecord> records = noticeRecordBuilder.buildLogRecord(payload).get();

    assertThat(records.size(), equalTo(1));

    LogRecord noticeLogRecord = records.get(0);
    assertThat(noticeLogRecord.getUserBarcode(), equalTo("605566117903595"));
    assertThat(noticeLogRecord.getItems().size(), equalTo(1));
    assertThat(noticeLogRecord.getItems().get(0).getItemBarcode(), equalTo("90000"));
    assertThat(noticeLogRecord.getItems().get(0).getItemId(), equalTo("100d10bf-2f06-4aa0-be15-0b95b2d9f9e3"));
    assertThat(noticeLogRecord.getObject(), equalTo(LogRecord.Object.NOTICE));
    assertThat(noticeLogRecord.getAction(), equalTo(SEND));
    assertThat(noticeLogRecord.getDate(), is(not(nullValue())));
    assertThat(noticeLogRecord.getServicePointId(), equalTo("7c5abc9f-f3d7-4856-b8d7-6712462ca007"));
    assertThat(noticeLogRecord.getLinkToIds().getFeeFineId(), equalTo("7ad9dfa0-6ee9-43ba-8db5-7a034ce05838"));
    assertThat(noticeLogRecord.getLinkToIds().getUserId(), equalTo("61da1f1b-eb9e-5a33-9d47-617c84191d12"));
    assertThat(noticeLogRecord.getLinkToIds().getTemplateId(), equalTo("21f4fd8e-cf19-4870-8288-eb675aec8b3c"));
    assertThat(noticeLogRecord.getSource(), equalTo("System"));
    assertThat(noticeLogRecord.getDescription(),
      equalTo("Template: sample template. Triggering event: manual charge."));
  }

  @Test
  void testNoticeErrorEventWithFullData() throws Exception {
    logger.info("===== Test notice error log record builder for event with full data =====");

    JsonObject payload = new JsonObject(getFile(NOTICE_ERROR_FULL_PAYLOAD_JSON));

    List<LogRecord> records = noticeErrorRecordBuilder.buildLogRecord(payload).get();

    assertThat(records.size(), equalTo(1));

    LogRecord logRecord = records.get(0);

    assertThat(logRecord.getUserBarcode(), equalTo("605566117903595"));
    assertThat(logRecord.getSource(), equalTo("System"));
    assertThat(logRecord.getObject(), equalTo(LogRecord.Object.NOTICE));
    assertThat(logRecord.getAction(), equalTo(SEND_ERROR));
    assertThat(logRecord.getDate(), is(not(nullValue())));
    assertThat(logRecord.getServicePointId(), equalTo("3a40852d-49fd-4df2-a1f9-6e2641a6e91f"));

    assertThat(logRecord.getItems(), hasSize(1));
    Item item = logRecord.getItems().get(0);

    assertThat(item.getItemBarcode(), equalTo("90000"));
    assertThat(item.getItemId(), equalTo("7212ba6a-8dcf-45a1-be9a-ffaa847c4423"));
    assertThat(item.getInstanceId(), equalTo("5bf370e0-8cca-4d9c-82e4-5170ab2a0a39"));
    assertThat(item.getHoldingId(), equalTo("e3ff6133-b9a2-4d4c-a1c9-dc1867d4df19"));
    assertThat(item.getLoanId(), equalTo("27e9085d-305a-455a-a375-c6c87a388a6a"));

    assertThat(logRecord.getLinkToIds().getUserId(), equalTo("61da1f1b-eb9e-5a33-9d47-617c84191d12"));
    assertThat(logRecord.getLinkToIds().getTemplateId(), equalTo("21f4fd8e-cf19-4870-8288-eb675aec8b3c"));
    assertThat(logRecord.getLinkToIds().getNoticePolicyId(), equalTo("60c7d3c0-6786-449d-bd28-d7449667cfde"));
    assertThat(logRecord.getDescription(), equalTo(
      "Template: sample template. Triggering event: Due date. Error message: Server error failure"));
  }

  @Test
  void testNoticeErrorEventWithMinimalData() throws Exception {
    logger.info("===== Test notice error log record builder for event with minimal data =====");

    JsonObject payload = new JsonObject(getFile(NOTICE_ERROR_MINIMAL_PAYLOAD_JSON));

    List<LogRecord> records = noticeErrorRecordBuilder.buildLogRecord(payload).get();

    assertThat(records.size(), equalTo(1));

    LogRecord logRecord = records.get(0);

    assertThat(logRecord.getUserBarcode(), equalTo("605566117903595"));
    assertThat(logRecord.getSource(), equalTo("System"));
    assertThat(logRecord.getObject(), equalTo(LogRecord.Object.NOTICE));
    assertThat(logRecord.getAction(), equalTo(SEND_ERROR));
    assertThat(logRecord.getDate(), is(not(nullValue())));
    assertThat(logRecord.getServicePointId(), is(nullValue()));

    assertThat(logRecord.getItems(), hasSize(0));

    assertThat(logRecord.getLinkToIds().getUserId(), equalTo("61da1f1b-eb9e-5a33-9d47-617c84191d12"));
    assertThat(logRecord.getLinkToIds().getTemplateId(), is(nullValue()));
    assertThat(logRecord.getLinkToIds().getNoticePolicyId(), is(nullValue()));
    assertThat(logRecord.getLinkToIds().getFeeFineId(), is(nullValue()));
    assertThat(logRecord.getLinkToIds().getRequestId(), is(nullValue()));
    assertThat(logRecord.getLinkToIds().getNoticePolicyId(), is(nullValue()));
    assertThat(logRecord.getDescription(), equalTo(
      "Template: unknown. Triggering event: unknown. Error message: unknown"));
  }
}
