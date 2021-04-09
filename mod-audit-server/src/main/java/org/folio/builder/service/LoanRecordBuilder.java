package org.folio.builder.service;

import static org.folio.rest.jaxrs.model.LogRecord.Action.AGE_TO_LOST;
import static org.folio.rest.jaxrs.model.LogRecord.Action.ANONYMIZE;
import static org.folio.rest.jaxrs.model.LogRecord.Object.LOAN;
import static org.folio.util.Constants.SYSTEM;
import static org.folio.util.JsonPropertyFetcher.getDateTimeProperty;
import static org.folio.util.JsonPropertyFetcher.getObjectProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.ACTION;
import static org.folio.util.LogEventPayloadField.DATE;
import static org.folio.util.LogEventPayloadField.DESCRIPTION;
import static org.folio.util.LogEventPayloadField.HOLDINGS_RECORD_ID;
import static org.folio.util.LogEventPayloadField.INSTANCE_ID;
import static org.folio.util.LogEventPayloadField.ITEM_BARCODE;
import static org.folio.util.LogEventPayloadField.ITEM_ID;
import static org.folio.util.LogEventPayloadField.LOAN_ID;
import static org.folio.util.LogEventPayloadField.PAYLOAD;
import static org.folio.util.LogEventPayloadField.PERSONAL_NAME;
import static org.folio.util.LogEventPayloadField.SERVICE_POINT_ID;
import static org.folio.util.LogEventPayloadField.UPDATED_BY_USER_ID;
import static org.folio.util.LogEventPayloadField.USER_BARCODE;
import static org.folio.util.LogEventPayloadField.USER_ID;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.LinkToIds;
import org.folio.rest.jaxrs.model.LogRecord;

import io.vertx.core.Context;
import io.vertx.core.json.JsonObject;

public class LoanRecordBuilder extends LogRecordBuilder {

  public LoanRecordBuilder(Map<String, String> okapiHeaders, Context vertxContext) {
    super(okapiHeaders, vertxContext);
  }

  @Override
  public CompletableFuture<List<LogRecord>> buildLogRecord(JsonObject fullPayload) {
    JsonObject payload = getObjectProperty(fullPayload, PAYLOAD);

    if (isAction(payload, ANONYMIZE)) {
      return fetchItemDetails(payload)
        .thenCompose(this::createResult);
    } else if (isAction(payload, AGE_TO_LOST)) {
      return fetchUserDetails(payload, getProperty(payload, USER_ID))
        .thenCompose(this::createResult);
    }
    return fetchUserDetails(payload, getProperty(payload, UPDATED_BY_USER_ID))
      .thenCompose(this::createResult);
  }

  private CompletableFuture<List<LogRecord>> createResult(JsonObject payload) {
    return CompletableFuture.completedFuture(Collections.singletonList(new LogRecord()
      .withObject(LOAN)
      .withUserBarcode(getProperty(payload, USER_BARCODE))
      .withItems(Collections.singletonList(new Item()
        .withItemId(getProperty(payload, ITEM_ID))
        .withItemBarcode(getProperty(payload, ITEM_BARCODE))
        .withInstanceId(getProperty(payload, INSTANCE_ID))
        .withHoldingId(getProperty(payload, HOLDINGS_RECORD_ID))
        .withLoanId(getProperty(payload, LOAN_ID))))
      .withAction(resolveAction(getProperty(payload, ACTION)))
      .withDate(getDateTimeProperty(payload, DATE).toDate())
      .withServicePointId(isAction(payload, AGE_TO_LOST) ? null : getProperty(payload, SERVICE_POINT_ID))
      .withSource(isAction(payload, ANONYMIZE) || isAction(payload, AGE_TO_LOST) ? SYSTEM : getProperty(payload, PERSONAL_NAME))
      .withDescription(getProperty(payload, DESCRIPTION))
      .withLinkToIds(new LinkToIds().withUserId(isAction(payload, ANONYMIZE) ? null : getProperty(payload, USER_ID)))));
  }

  private boolean isAction(JsonObject payload, LogRecord.Action action) {
    return action.equals(resolveAction(getProperty(payload, ACTION)));
  }
}
