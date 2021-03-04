package org.folio.builder.service;

import static org.folio.rest.jaxrs.model.LogRecord.Action.BILLED;
import static org.folio.utils.TenantApiTestUtil.FEE_FINE_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.getFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.builder.description.FeeFineDescriptionBuilder;
import org.folio.rest.jaxrs.model.LogRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.vertx.core.json.JsonObject;

public class FeeFineRecordBuilderTest extends BuilderTestBase {
  private static final Logger logger = LogManager.getLogger();

  @Test
  void testFeeFine() throws Exception {
    logger.info("===== Test fees/fines log records builder =====");

    JsonObject payload = new JsonObject(getFile(FEE_FINE_PAYLOAD_JSON));
    List<LogRecord> records = feeFineRecordBuilder.buildLogRecord(payload).get();

    assertThat(records.size(), equalTo(1));

    LogRecord feeFineLogRecord = records.get(0);
    assertThat(feeFineLogRecord.getUserBarcode(), equalTo("693787594998493"));
    assertThat(feeFineLogRecord.getItems().size(), equalTo(1));
    assertThat(feeFineLogRecord.getItems().get(0).getItemBarcode(), equalTo("90000"));
    assertThat(feeFineLogRecord.getItems().get(0).getItemId(), equalTo("100d10bf-2f06-4aa0-be15-0b95b2d9f9e3"));
    assertThat(feeFineLogRecord.getItems().get(0).getInstanceId(), equalTo("5bf370e0-8cca-4d9c-82e4-5170ab2a0a39"));
    assertThat(feeFineLogRecord.getItems().get(0).getHoldingId(), equalTo("e3ff6133-b9a2-4d4c-a1c9-dc1867d4df19"));
    assertThat(feeFineLogRecord.getObject(), equalTo(LogRecord.Object.FEE_FINE));
    assertThat(feeFineLogRecord.getAction(), equalTo(BILLED));
    assertThat(feeFineLogRecord.getDate(), is(not(nullValue())));
    assertThat(feeFineLogRecord.getServicePointId(), equalTo("7c5abc9f-f3d7-4856-b8d7-6712462ca007"));
    assertThat(feeFineLogRecord.getSource(), equalTo("ADMINISTRATOR, DIKU"));
    assertThat(feeFineLogRecord.getDescription(),
      equalTo("Fee/Fine type: manual charge. Fee/Fine owner: sample owner. Amount: 10.00. manual"));
  }

  @ParameterizedTest
  @EnumSource(FeeFineDescriptions.class)
  void testDescriptions(FeeFineDescriptions feeFineDescription) {
    String actual = new FeeFineDescriptionBuilder().buildDescription(new JsonObject(feeFineDescription.getPayload()));
    assertThat(feeFineDescription.getExpected(), equalTo(actual));
  }
}
