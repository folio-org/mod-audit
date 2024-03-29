package org.folio.builder.service;

import static java.util.Optional.ofNullable;
import static org.folio.rest.jaxrs.model.LogRecord.Object.FEE_FINE;
import static org.folio.util.Constants.UUID_PATTERN;
import static org.folio.util.JsonPropertyFetcher.getDateTimeProperty;
import static org.folio.util.JsonPropertyFetcher.getObjectProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.ACCOUNT_ID;
import static org.folio.util.LogEventPayloadField.ACTION;
import static org.folio.util.LogEventPayloadField.DATE;
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

import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.builder.description.FeeFineDescriptionBuilder;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.LinkToIds;
import org.folio.rest.jaxrs.model.LogRecord;

import io.vertx.core.Context;
import io.vertx.core.json.JsonObject;

public class FeeFineRecordBuilder extends LogRecordBuilder {
  private static final Logger LOGGER = LogManager.getLogger();

  public FeeFineRecordBuilder(Map<String, String> okapiHeaders, Context vertxContext) {
    super(okapiHeaders, vertxContext);
  }

  @Override
  public CompletableFuture<List<LogRecord>> buildLogRecord(JsonObject fullPayload) {
    LOGGER.debug("buildLogRecord:: Building Log Record");
    JsonObject payload = getObjectProperty(fullPayload, PAYLOAD);

    LOGGER.info("buildLogRecord:: Built Log Record");
    return fetchUserDetails(payload, getProperty(payload, USER_ID))
      .thenCompose(this::fetchItemDetails)
      .thenCompose(this::createResult);
  }

  private CompletableFuture<List<LogRecord>> createResult(JsonObject payload) {
    LOGGER.debug("createResult:: Creating Result");
    Item item = new Item()
      .withItemId(getProperty(payload, ITEM_ID))
      .withItemBarcode(getProperty(payload, ITEM_BARCODE))
      .withInstanceId(getProperty(payload, INSTANCE_ID))
      .withHoldingId(getProperty(payload, HOLDINGS_RECORD_ID));

    ofNullable(getProperty(payload, LOAN_ID)).ifPresent(id -> {
      if (id.matches(UUID_PATTERN)) {
        item.setLoanId(id);
      }
    });

    LOGGER.info("createResult:: Created Result");
    return CompletableFuture.completedFuture(Collections.singletonList(new LogRecord()
      .withObject(FEE_FINE)
      .withUserBarcode(getProperty(payload, USER_BARCODE))
      .withItems(Collections.singletonList(item))
      .withAction(resolveAction(getProperty(payload, ACTION)))
      .withDate(Date.from(getDateTimeProperty(payload, DATE).toInstant(ZoneOffset.UTC)))
      .withServicePointId(getProperty(payload, SERVICE_POINT_ID))
      .withSource(getProperty(payload, SOURCE))
      .withDescription(new FeeFineDescriptionBuilder().buildDescription(payload))
      .withLinkToIds(new LinkToIds()
        .withUserId(getProperty(payload, USER_ID))
        .withFeeFineId(getProperty(payload, ACCOUNT_ID)))));
  }

}
