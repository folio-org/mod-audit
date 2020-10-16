package org.folio.builder.service;

import static org.folio.rest.jaxrs.model.LogRecord.Action.BILLED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.builder.description.FeeFineDescriptionBuilder;
import org.folio.rest.jaxrs.model.LogRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

public class FeeFineRecordBuilderTest extends BuilderTestBase {
  private static final Logger logger = LoggerFactory.getLogger(FeeFineDescriptionBuilder.class);

  @Test
  void testFeeFine() {
    logger.info("===== Test fees/fines log records builder =====");

    JsonObject payload = new JsonObject(getFile(FEE_FINE_PAYLOAD_JSON));
    List<LogRecord> records = feeFineRecordBuilder.buildLogRecord(payload);

    assertThat(records.size(), equalTo(1));

    LogRecord feeFineLogRecord = records.get(0);
    assertThat(feeFineLogRecord.getUserBarcode(), equalTo("693787594998493"));
    assertThat(feeFineLogRecord.getItems().size(), equalTo(1));
    assertThat(feeFineLogRecord.getItems().get(0).getItemBarcode(), equalTo("90000"));
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
