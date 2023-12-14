package org.folio.builder.service;

import static org.folio.util.JsonPropertyFetcher.getArrayProperty;
import static org.folio.util.JsonPropertyFetcher.getBooleanProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.HOLDINGS_RECORD_ID;
import static org.folio.util.LogEventPayloadField.INSTANCE_ID;
import static org.folio.util.LogEventPayloadField.IS_LOAN_CLOSED;
import static org.folio.util.LogEventPayloadField.ITEM_BARCODE;
import static org.folio.util.LogEventPayloadField.ITEM_ID;
import static org.folio.util.LogEventPayloadField.LOAN_ID;
import static org.folio.util.LogEventPayloadField.REQUESTS;
import static org.folio.util.LogEventPayloadField.REQUEST_ID;
import static org.folio.util.LogEventPayloadField.SERVICE_POINT_ID;
import static org.folio.util.LogEventPayloadField.SOURCE;
import static org.folio.util.LogEventPayloadField.USER_BARCODE;
import static org.folio.util.LogEventPayloadField.USER_ID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.builder.description.DescriptionBuilder;
import org.folio.builder.description.ItemCheckInDescriptionBuilder;
import org.folio.builder.description.LoanCheckInDescriptionBuilder;
import org.folio.builder.description.RequestStatusChangedDescriptionBuilder;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.LinkToIds;
import org.folio.rest.jaxrs.model.LogRecord;

import io.vertx.core.Context;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class CheckInRecordBuilder extends LogRecordBuilder {

  private static final Logger LOGGER = LogManager.getLogger();

  private final DescriptionBuilder itemCheckInDescriptionBuilder = new ItemCheckInDescriptionBuilder();

  public CheckInRecordBuilder(Map<String, String> okapiHeaders, Context vertxContext) {
    super(okapiHeaders, vertxContext);
  }

  @Override
  public CompletableFuture<List<LogRecord>> buildLogRecord(JsonObject payload) {
    LOGGER.debug("buildLogRecord:: Building Log Record");
    List<LogRecord> logRecords = new ArrayList<>();
    logRecords.add(buildItemCheckInRecord(payload));
    LOGGER.info("buildLogRecord:: Item check-in record added to log records");

    if (getBooleanProperty(payload, IS_LOAN_CLOSED)) {
      logRecords.add(buildLoanCheckInRecord(payload));
      LOGGER.info("buildLogRecord:: Loan check-in record added to log records");
    }

    JsonArray requests = getArrayProperty(payload, REQUESTS);
    for (int i = 0; i < requests.size(); i++) {
      logRecords.add(buildCheckInRequestStatusChangedRecord(payload, requests.getJsonObject(i)));
    }
    LOGGER.info("buildLogRecord:: Built Log Record");
    return CompletableFuture.completedFuture(logRecords);
  }

  private LogRecord buildLoanCheckInRecord(JsonObject payload) {
    LOGGER.debug("buildLoanCheckInRecord:: Building loan check-in record");
    return new LogRecord().withObject(LogRecord.Object.LOAN)
      .withAction(LogRecord.Action.CLOSED_LOAN)
      .withUserBarcode(getProperty(payload, USER_BARCODE))
      .withSource(getProperty(payload, SOURCE))
      .withItems(buildItems(payload))
      .withServicePointId(getProperty(payload, SERVICE_POINT_ID))
      .withDate(new Date())
      .withLinkToIds(new LinkToIds().withUserId(getProperty(payload, USER_ID)))
      .withDescription(new LoanCheckInDescriptionBuilder().buildDescription(payload));
  }

  private LogRecord buildItemCheckInRecord(JsonObject payload) {
    LOGGER.debug("buildItemCheckInRecord:: Building item check-in record");
    return new LogRecord().withObject(LogRecord.Object.N_A)
      .withAction(LogRecord.Action.CHECKED_IN)
      .withUserBarcode(getProperty(payload, USER_BARCODE))
      .withSource(getProperty(payload, SOURCE))
      .withItems(buildItems(payload))
      .withServicePointId(getProperty(payload, SERVICE_POINT_ID))
      .withDate(new Date())
      .withLinkToIds(new LinkToIds().withUserId(getProperty(payload, USER_ID)))
      .withDescription(itemCheckInDescriptionBuilder.buildDescription(payload));
  }

  private LogRecord buildCheckInRequestStatusChangedRecord(JsonObject payload, JsonObject request) {
    LOGGER.debug("buildCheckInRequestStatusChangedRecord:: Building check-in request status changed record");
    return new LogRecord().withObject(LogRecord.Object.REQUEST)
      .withAction(LogRecord.Action.REQUEST_STATUS_CHANGED)
      .withUserBarcode(getProperty(payload, USER_BARCODE))
      .withSource(getProperty(payload, SOURCE))
      .withItems(buildItems(payload))
      .withServicePointId(getProperty(payload, SERVICE_POINT_ID))
      .withDate(new Date())
      .withLinkToIds(new LinkToIds().withUserId(getProperty(payload, USER_ID))
        .withRequestId(getProperty(request, REQUEST_ID)))
      .withDescription(new RequestStatusChangedDescriptionBuilder().buildDescription(request));
  }

  private List<Item> buildItems(JsonObject payload) {
    LOGGER.debug("buildItems:: Building items");
    return Collections.singletonList(new Item().withItemId(getProperty(payload, ITEM_ID))
      .withItemBarcode(getProperty(payload, ITEM_BARCODE))
      .withHoldingId(getProperty(payload, HOLDINGS_RECORD_ID))
      .withInstanceId(getProperty(payload, INSTANCE_ID))
      .withLoanId(getProperty(payload, LOAN_ID)));
  }
}
