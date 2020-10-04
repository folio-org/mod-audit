package org.folio.builder.record;

import static org.folio.util.JsonPropertyFetcher.getArrayProperty;
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

import org.folio.builder.description.LoanCheckOutDescriptionBuilder;
import org.folio.builder.description.RequestStatusChangedDescriptionBuilder;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.LinkToIds;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.util.JsonPropertyFetcher;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class CheckOutRecordBuilder implements LogRecordBuilder {

  @Override
  public List<LogRecord> buildLogRecord(JsonObject payload) {
    List<LogRecord> logRecords = new ArrayList<>();
    logRecords.add(buildLoanCheckOutRecord(payload));

    JsonArray requests = getArrayProperty(payload, REQUESTS);
    for (int i = 0; i < requests.size(); i++) {
      logRecords.add(buildCheckOutRequestStatusChangedRecord(payload, requests.getJsonObject(i)));
    }

    return logRecords;
  }

  private LogRecord buildCheckOutRequestStatusChangedRecord(JsonObject payload, JsonObject request) {
    return new LogRecord().withObject(LogRecord.Object.REQUEST)
      .withAction(LogRecord.Action.REQUEST_STATUS_CHANGED)
      .withUserBarcode(JsonPropertyFetcher.getProperty(payload, USER_BARCODE))
      .withSource(JsonPropertyFetcher.getProperty(payload, SOURCE))
      .withItems(buildItems(payload))
      .withServicePointId(JsonPropertyFetcher.getProperty(payload, SERVICE_POINT_ID))
      .withDate(new Date())
      .withDescription(new RequestStatusChangedDescriptionBuilder().buildDescription(request))
      .withLinkToIds(new LinkToIds().withUserId(JsonPropertyFetcher.getProperty(payload, USER_ID))
        .withRequestId(JsonPropertyFetcher.getProperty(request, REQUEST_ID)));
  }

  private LogRecord buildLoanCheckOutRecord(JsonObject payload) {
    return new LogRecord().withObject(LogRecord.Object.ITEM)
      .withAction(LogRecord.Action.CHECKED_OUT)
      .withUserBarcode(JsonPropertyFetcher.getProperty(payload, USER_BARCODE))
      .withSource(JsonPropertyFetcher.getProperty(payload, SOURCE))
      .withItems(buildItems(payload))
      .withServicePointId(JsonPropertyFetcher.getProperty(payload, SERVICE_POINT_ID))
      .withDate(new Date())
      .withDescription(new LoanCheckOutDescriptionBuilder().buildDescription(payload))
      .withLinkToIds(new LinkToIds().withUserId(JsonPropertyFetcher.getProperty(payload, USER_ID)));
  }

  private List<Item> buildItems(JsonObject payload) {
    return Collections.singletonList(new Item().withItemId(JsonPropertyFetcher.getProperty(payload, ITEM_ID))
      .withItemBarcode(JsonPropertyFetcher.getProperty(payload, ITEM_BARCODE))
      .withLoanId(JsonPropertyFetcher.getProperty(payload, LOAN_ID)));
  }

}
