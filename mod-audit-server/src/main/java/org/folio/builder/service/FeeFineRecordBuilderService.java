package org.folio.builder.service;

import static java.util.Optional.ofNullable;
import static org.folio.rest.jaxrs.model.LogRecord.Object.FEE_FINE;
import static org.folio.util.Constants.uuidPattern;
import static org.folio.util.JsonPropertyFetcher.getDateTimeProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.ACTION;
import static org.folio.util.LogEventPayloadField.DATE;
import static org.folio.util.LogEventPayloadField.FEE_FINE_ID;
import static org.folio.util.LogEventPayloadField.HOLDINGS_RECORD_ID;
import static org.folio.util.LogEventPayloadField.INSTANCE_ID;
import static org.folio.util.LogEventPayloadField.ITEM_BARCODE;
import static org.folio.util.LogEventPayloadField.ITEM_ID;
import static org.folio.util.LogEventPayloadField.LOAN_ID;
import static org.folio.util.LogEventPayloadField.PAYLOAD;
import static org.folio.util.LogEventPayloadField.SERVICE_POINT_ID;
import static org.folio.util.LogEventPayloadField.SOURCE;
import static org.folio.util.LogEventPayloadField.USER_BARCODE;
import static org.folio.util.LogEventPayloadField.USER_ID;

import io.vertx.core.json.JsonObject;
import org.folio.builder.description.FeeFineDescriptionBuilder;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.LinkToIds;
import org.folio.rest.jaxrs.model.LogRecord;

import java.util.Collections;
import java.util.List;

public class FeeFineRecordBuilderService extends LogRecordBuilderService {
  @Override
  public List<LogRecord> buildLogRecord(JsonObject fullPayload) {
    JsonObject payload = new JsonObject(getProperty(fullPayload, PAYLOAD));

    Item item = new Item()
      .withItemId(getProperty(payload, ITEM_ID))
      .withItemBarcode(getProperty(payload, ITEM_BARCODE))
      .withInstanceId(getProperty(payload, INSTANCE_ID))
      .withHoldingId(getProperty(payload, HOLDINGS_RECORD_ID));

    ofNullable(getProperty(payload, LOAN_ID)).ifPresent(id -> {
      if (id.matches(uuidPattern)) {
        item.setLoanId(id);
      }
    });

    return Collections.singletonList(new LogRecord()
      .withObject(FEE_FINE)
      .withUserBarcode(getProperty(payload, USER_BARCODE))
      .withItems(Collections.singletonList(item))
      .withAction(LogRecord.Action.fromValue(getProperty(payload, ACTION)))
      .withDate(getDateTimeProperty(payload, DATE).toDate())
      .withServicePointId(getProperty(payload, SERVICE_POINT_ID))
      .withSource(getProperty(payload, SOURCE))
      .withDescription(new FeeFineDescriptionBuilder().buildDescription(payload))
      .withLinkToIds(new LinkToIds()
        .withUserId(getProperty(payload, USER_ID))
        .withFeeFineId(getProperty(payload, FEE_FINE_ID))));
  }
}
