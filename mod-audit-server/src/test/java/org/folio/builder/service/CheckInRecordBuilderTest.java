package org.folio.builder.service;

import static java.lang.String.format;
import static java.util.stream.Collectors.groupingBy;
import static org.folio.builder.description.DescriptionHelper.getFormattedDateTime;
import static org.folio.util.JsonPropertyFetcher.getArrayProperty;
import static org.folio.util.JsonPropertyFetcher.getDateTimeProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.DESTINATION_SERVICE_POINT;
import static org.folio.util.LogEventPayloadField.DUE_DATE;
import static org.folio.util.LogEventPayloadField.ITEM_STATUS_NAME;
import static org.folio.util.LogEventPayloadField.NEW_REQUEST_STATUS;
import static org.folio.util.LogEventPayloadField.OLD_REQUEST_STATUS;
import static org.folio.util.LogEventPayloadField.REQUESTS;
import static org.folio.util.LogEventPayloadField.REQUEST_TYPE;
import static org.folio.util.LogEventPayloadField.RETURN_DATE;
import static org.folio.util.LogEventPayloadField.ZONE_ID;
import static org.folio.utils.TenantApiTestUtil.CHECK_IN_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.CHECK_IN_WITH_BACKDATE_TIMEZONE_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.CHECK_IN_WITH_TIMEZONE_PAYLOAD_JSON;
import static org.folio.utils.TenantApiTestUtil.getFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.LogRecord;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


public class CheckInRecordBuilderTest extends BuilderTestBase {

  private static final Logger logger = LogManager.getLogger();

  @ParameterizedTest
  @ValueSource(strings = {CHECK_IN_PAYLOAD_JSON, CHECK_IN_WITH_TIMEZONE_PAYLOAD_JSON, CHECK_IN_WITH_BACKDATE_TIMEZONE_PAYLOAD_JSON })
  void checkInTest(String sample) throws Exception {
    logger.info("Test check-in log records builder");

    JsonObject payload = new JsonObject(getFile(sample));

    Map<LogRecord.Object, Map<LogRecord.Action, List<LogRecord>>> records = checkInRecordBuilder.buildLogRecord(payload)
      .get().stream()
      .collect(groupingBy(LogRecord::getObject, groupingBy(LogRecord::getAction)));

    assertThat(records.entrySet(), hasSize(3));
    assertThat(records.get(LogRecord.Object.N_A).get(LogRecord.Action.CHECKED_IN), hasSize(1));
    assertThat(records.get(LogRecord.Object.LOAN).get(LogRecord.Action.CLOSED_LOAN), hasSize(1));
    assertThat(records.get(LogRecord.Object.REQUEST).get(LogRecord.Action.REQUEST_STATUS_CHANGED), hasSize(1));

    LogRecord itemCheckedInRecord = records.get(LogRecord.Object.N_A).get(LogRecord.Action.CHECKED_IN).get(0);
    validateBaseContent(payload, itemCheckedInRecord);
    validateAdditionalContent(payload, itemCheckedInRecord);
    assertThat(itemCheckedInRecord.getDescription(), equalTo(format("Item status: %s to %s.",
        getProperty(payload, ITEM_STATUS_NAME), getProperty(payload, DESTINATION_SERVICE_POINT))));

    LogRecord loanClosedRecord = records.get(LogRecord.Object.LOAN).get(LogRecord.Action.CLOSED_LOAN).get(0);
    validateAdditionalContent(payload, loanClosedRecord);

    if(!sample.equalsIgnoreCase(CHECK_IN_WITH_TIMEZONE_PAYLOAD_JSON)) {
      assertThat(loanClosedRecord.getDescription(), equalTo(format("Item status: %s. Backdated to: %s. Overdue due date: %s.",
        getProperty(payload, ITEM_STATUS_NAME),
        getFormattedDateTime(getDateInTenantTimeZone(getDateTimeProperty(payload, RETURN_DATE),
          ZoneId.of(getProperty(payload, ZONE_ID) != null ? getProperty(payload, ZONE_ID) : ZoneOffset.UTC.getId())).toLocalDateTime()),
        getFormattedDateTime(getDateTimeProperty(payload, DUE_DATE)))));
    }

    LogRecord requestStatusChangedRecord = records.get(LogRecord.Object.REQUEST).get(LogRecord.Action.REQUEST_STATUS_CHANGED).get(0);
    validateBaseContent(payload, requestStatusChangedRecord);
    validateRequestStatusChangedContent(payload, requestStatusChangedRecord);

    JsonArray requests = getArrayProperty(payload, REQUESTS);
    JsonObject request = requests.getJsonObject(0);

    String requestType = getProperty(request, REQUEST_TYPE);
    String oldRequestStatus = getProperty(request, OLD_REQUEST_STATUS);
    String newRequestStatus = getProperty(request, NEW_REQUEST_STATUS);

    assertThat(requestStatusChangedRecord.getDescription(),
        equalTo(format("Type: %s. New request status: %s (from: %s).", requestType, newRequestStatus, oldRequestStatus)));
  }

  private ZonedDateTime getDateInTenantTimeZone(LocalDateTime localDateTime, ZoneId zoneId) {
    return  localDateTime.atZone(ZoneId.of(ZoneOffset.UTC.getId())).withZoneSameInstant(zoneId);
  }
}
