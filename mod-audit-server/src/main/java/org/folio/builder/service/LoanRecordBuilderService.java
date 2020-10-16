package org.folio.builder.service;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.folio.rest.jaxrs.model.LogRecord.Object.LOAN;
import static org.folio.util.JsonPropertyFetcher.getDateTimeProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.ACTION;
import static org.folio.util.LogEventPayloadField.DATE;
import static org.folio.util.LogEventPayloadField.DESCRIPTION;
import static org.folio.util.LogEventPayloadField.HOLDINGS_RECORD_ID;
import static org.folio.util.LogEventPayloadField.INSTANCE_ID;
import static org.folio.util.LogEventPayloadField.ITEM_BARCODE;
import static org.folio.util.LogEventPayloadField.ITEM_ID;
import static org.folio.util.LogEventPayloadField.LOAN_ID;
import static org.folio.util.LogEventPayloadField.SERVICE_POINT_ID;
import static org.folio.util.LogEventPayloadField.USER_BARCODE;
import static org.folio.util.LogEventPayloadField.USER_ID;

import io.vertx.core.Context;
import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.LinkToIds;
import org.folio.rest.jaxrs.model.LogRecord;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LoanRecordBuilderService extends LogRecordBuilderService {
  public LoanRecordBuilderService(Context context, Map<String, String> headers) {
    super(context, headers);
  }

  @Override
  public List<LogRecord> buildLogRecord(JsonObject payload) {
    return Collections.singletonList(new LogRecord()
      .withObject(LOAN)
      .withUserBarcode(getProperty(payload, USER_BARCODE))
      .withItems(Collections.singletonList(new Item()
        .withItemId(getProperty(payload, ITEM_ID))
        .withItemBarcode(getProperty(payload, ITEM_BARCODE))
        .withInstanceId(getProperty(payload, INSTANCE_ID))
        .withHoldingId(getProperty(payload, HOLDINGS_RECORD_ID))
        .withLoanId(getProperty(payload, LOAN_ID))))
      .withAction(LogRecord.Action.fromValue(getProperty(payload, ACTION)))
      .withDate(getDateTimeProperty(payload, DATE).toDate())
      .withServicePointId(getProperty(payload, SERVICE_POINT_ID))
      .withSource(EMPTY)
      .withDescription(getProperty(payload, DESCRIPTION))
      .withLinkToIds(new LinkToIds().withUserId(getProperty(payload, USER_ID))));
  }
}
