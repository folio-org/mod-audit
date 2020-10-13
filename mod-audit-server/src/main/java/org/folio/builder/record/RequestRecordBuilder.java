package org.folio.builder.record;

import io.vertx.core.json.JsonObject;
import org.folio.builder.description.request.RequestCreatedDescriptionBuilder;
import org.folio.builder.description.request.RequestStatusChangedDescriptionBuilder;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.LinkToIds;
import org.folio.rest.jaxrs.model.LogRecord;
import org.folio.util.JsonPropertyFetcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.folio.util.JsonPropertyFetcher.getNestedObjectProperty;
import static org.folio.util.JsonPropertyFetcher.getNestedStringProperty;
import static org.folio.util.JsonPropertyFetcher.getObjectProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.BARCODE;
import static org.folio.util.LogEventPayloadField.CREATED;
import static org.folio.util.LogEventPayloadField.HOLDINGS_RECORD_ID;
import static org.folio.util.LogEventPayloadField.INSTANCE_ID;
import static org.folio.util.LogEventPayloadField.ITEM;
import static org.folio.util.LogEventPayloadField.ITEM_BARCODE;
import static org.folio.util.LogEventPayloadField.ITEM_ID;
import static org.folio.util.LogEventPayloadField.LOAN_ID;
import static org.folio.util.LogEventPayloadField.PAYLOAD;
import static org.folio.util.LogEventPayloadField.REQUESTER;
import static org.folio.util.LogEventPayloadField.REQUESTS;
import static org.folio.util.LogEventPayloadField.REQUEST_ID;
import static org.folio.util.LogEventPayloadField.SERVICE_POINT_ID;
import static org.folio.util.LogEventPayloadField.SOURCE;
import static org.folio.util.LogEventPayloadField.USER_BARCODE;
import static org.folio.util.LogEventPayloadField.USER_ID;

public class RequestRecordBuilder implements LogRecordBuilder {
  @Override
  public List<LogRecord> buildLogRecord(JsonObject event) {
    List<LogRecord> logRecords = new ArrayList<>();


    JsonObject payload = getNestedObjectProperty(getObjectProperty(event, PAYLOAD), REQUESTS, CREATED);

    logRecords.add(buildRequestCreatedRecord(payload));

    return logRecords;
  }

  private LogRecord buildRequestCreatedRecord(JsonObject request) {
    return new LogRecord().withObject(LogRecord.Object.REQUEST)
      .withAction(LogRecord.Action.CREATED)
      .withUserBarcode(getNestedStringProperty(request, REQUESTER, BARCODE))
      .withItems(buildItems(request))
      .withDate(new Date())
      .withDescription(new RequestCreatedDescriptionBuilder().buildDescription(request))
      .withLinkToIds(new LinkToIds().withUserId(JsonPropertyFetcher.getProperty(request, USER_ID))
        .withRequestId(JsonPropertyFetcher.getProperty(request, REQUEST_ID)));
  }

  private List<Item> buildItems(JsonObject payload) {
    return Collections.singletonList(new Item().withItemId(getProperty(payload, ITEM_ID))
      .withItemBarcode(getNestedStringProperty(payload, ITEM, BARCODE))
      .withHoldingId(getProperty(payload, HOLDINGS_RECORD_ID))
      .withInstanceId(getProperty(payload, INSTANCE_ID))
      .withLoanId(getProperty(payload, LOAN_ID)));
  }
}
