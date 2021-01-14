package org.folio.builder.service;

import static org.folio.rest.jaxrs.model.LogRecord.Action.SEND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import io.vertx.core.json.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.LogRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

public class NoticeRecordBuilderTest extends BuilderTestBase {
  private static final Logger logger = LogManager.getLogger(NoticeRecordBuilderTest.class);

  @Test
  void testNotice() throws Exception {
    logger.info("===== Test notice log records builder =====");

    JsonObject payload = new JsonObject(getFile(NOTICE_PAYLOAD_JSON));
    List<LogRecord> records = noticeRecordBuilder.buildLogRecord(payload).get();

    assertThat(records.size(), equalTo(1));

    LogRecord noticeLogRecord = records.get(0);
    assertThat(noticeLogRecord.getUserBarcode(), equalTo("693787594998493"));
    assertThat(noticeLogRecord.getItems().size(), equalTo(1));
    assertThat(noticeLogRecord.getItems().get(0).getItemBarcode(), equalTo("90000"));
    assertThat(noticeLogRecord.getItems().get(0).getItemId(), equalTo("100d10bf-2f06-4aa0-be15-0b95b2d9f9e3"));
    assertThat(noticeLogRecord.getObject(), equalTo(LogRecord.Object.NOTICE));
    assertThat(noticeLogRecord.getAction(), equalTo(SEND));
    assertThat(noticeLogRecord.getDate(), is(not(nullValue())));
    assertThat(noticeLogRecord.getServicePointId(), equalTo("7c5abc9f-f3d7-4856-b8d7-6712462ca007"));
    assertThat(noticeLogRecord.getLinkToIds().getFeeFineId(), equalTo("7ad9dfa0-6ee9-43ba-8db5-7a034ce05838"));
    assertThat(noticeLogRecord.getSource(), equalTo("System"));
    assertThat(noticeLogRecord.getDescription(),
      equalTo("Template: sample template. Triggering event: manual charge."));
  }
}
