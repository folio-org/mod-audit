package org.folio.builder.service;

import static org.folio.util.JsonPropertyFetcher.getArrayProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.HOLDINGS_RECORD_ID;
import static org.folio.util.LogEventPayloadField.INSTANCE_ID;
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

import io.vertx.core.Context;
import org.folio.builder.description.LoanCheckOutDescriptionBuilder;
import org.folio.builder.description.RequestStatusChangedDescriptionBuilder;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.LinkToIds;
import org.folio.rest.jaxrs.model.LogRecord;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class CheckOutRecordBuilder extends LogRecordBuilder {
  public CheckOutRecordBuilder(Map<String, String> okapiHeaders, Context vertxContext) {
    super(okapiHeaders, vertxContext);
  }

  @Override
  public CompletableFuture<List<LogRecord>> buildLogRecord(JsonObject payload) {
    List<LogRecord> logRecords = new ArrayList<>();
    logRecords.add(buildLoanCheckOutRecord(payload));

    JsonArray requests = getArrayProperty(payload, REQUESTS);
    for (int i = 0; i < requests.size(); i++) {
      logRecords.add(buildCheckOutRequestStatusChangedRecord(payload, requests.getJsonObject(i)));
    }

    return CompletableFuture.completedFuture(logRecords);
  }

  private LogRecord buildCheckOutRequestStatusChangedRecord(JsonObject payload, JsonObject request) {
    return new LogRecord().withObject(LogRecord.Object.REQUEST)
      .withAction(LogRecord.Action.REQUEST_STATUS_CHANGED)
      .withUserBarcode(getProperty(payload, USER_BARCODE))
      .withSource(getProperty(payload, SOURCE))
      .withItems(buildItems(payload))
      .withServicePointId(getProperty(payload, SERVICE_POINT_ID))
      .withDate(new Date())
      .withDescription(new RequestStatusChangedDescriptionBuilder().buildDescription(request))
      .withLinkToIds(new LinkToIds().withUserId(getProperty(payload, USER_ID))
        .withRequestId(getProperty(request, REQUEST_ID)));
  }

  private LogRecord buildLoanCheckOutRecord(JsonObject payload) {
    return new LogRecord().withObject(LogRecord.Object.LOAN)
      .withAction(LogRecord.Action.CHECKED_OUT)
      .withUserBarcode(getProperty(payload, USER_BARCODE))
      .withSource(getProperty(payload, SOURCE))
      .withItems(buildItems(payload))
      .withServicePointId(getProperty(payload, SERVICE_POINT_ID))
      .withDate(new Date())
      .withDescription(new LoanCheckOutDescriptionBuilder().buildDescription(payload))
      .withLinkToIds(new LinkToIds().withUserId(getProperty(payload, USER_ID)));
  }

  private List<Item> buildItems(JsonObject payload) {
    return Collections.singletonList(new Item().withItemId(getProperty(payload, ITEM_ID))
      .withItemBarcode(getProperty(payload, ITEM_BARCODE))
      .withHoldingId(getProperty(payload, HOLDINGS_RECORD_ID))
      .withInstanceId(getProperty(payload, INSTANCE_ID))
      .withLoanId(getProperty(payload, LOAN_ID)));
  }

}
