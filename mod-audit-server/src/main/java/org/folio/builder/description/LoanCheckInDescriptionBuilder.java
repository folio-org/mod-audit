package org.folio.builder.description;

import static java.lang.String.format;
import static org.folio.builder.description.DescriptionHelper.getFormattedDateTime;
import static org.folio.builder.description.Descriptions.BACKDATED_TO_MSG;
import static org.folio.builder.description.Descriptions.CLAIMED_RETURNED_ITEM_MSG;
import static org.folio.builder.description.Descriptions.DOT_MSG;
import static org.folio.builder.description.Descriptions.ITEM_STATUS_MSG;
import static org.folio.builder.description.Descriptions.OVERDUE_DUE_DATE_MSG;
import static org.folio.util.JsonPropertyFetcher.getDateTimeProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.CLAIMED_RETURNED_RESOLUTION;
import static org.folio.util.LogEventPayloadField.DUE_DATE;
import static org.folio.util.LogEventPayloadField.ITEM_STATUS_NAME;
import static org.folio.util.LogEventPayloadField.RETURN_DATE;
import static org.folio.util.LogEventPayloadField.SYSTEM_RETURN_DATE;
import static org.folio.util.LogEventPayloadField.ZONE_ID;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.builder.ItemStatus;

import io.vertx.core.json.JsonObject;

public class LoanCheckInDescriptionBuilder implements DescriptionBuilder {
  private static final Logger log = LogManager.getLogger();
  @Override
  public String buildDescription(JsonObject logEventPayload) {
    StringBuilder description = new StringBuilder();
    ItemStatus status = ItemStatus.from(getProperty(logEventPayload, ITEM_STATUS_NAME));

    description.append(format(ITEM_STATUS_MSG, status.getValue()));

    if (status == ItemStatus.CLAIMED_RETURNED) {
      String claimedReturnedResolution = getProperty(logEventPayload, CLAIMED_RETURNED_RESOLUTION);
      description.append(CLAIMED_RETURNED_ITEM_MSG)
        .append(claimedReturnedResolution);
    }

    ZoneId zoneId = ZoneId.of(getProperty(logEventPayload, ZONE_ID));
    log.info("The payload zone ID " + getProperty(logEventPayload, ZONE_ID));
    log.info("The ZoneID created " + zoneId);

    ZonedDateTime returnDate = getDateInTenantTimeZone(getDateTimeProperty(logEventPayload, RETURN_DATE), zoneId);
    ZonedDateTime systemReturnDate = getDateInTenantTimeZone(getDateTimeProperty(logEventPayload, SYSTEM_RETURN_DATE), zoneId);
    ZonedDateTime dueDate = getDateInTenantTimeZone(getDateTimeProperty(logEventPayload, DUE_DATE), zoneId);

    log.info("Return Date " + returnDate);
    log.info("System Return Date " + systemReturnDate);

    if (returnDate.truncatedTo(ChronoUnit.MILLIS).compareTo(systemReturnDate.truncatedTo(ChronoUnit.MILLIS)) != 0) {
      description.append(BACKDATED_TO_MSG).append(getFormattedDateTime(returnDate.toLocalDateTime()));
    }

    if (dueDate.truncatedTo(ChronoUnit.MILLIS).isAfter(returnDate.truncatedTo(ChronoUnit.MILLIS))) {
      description.append(OVERDUE_DUE_DATE_MSG).append(getFormattedDateTime(dueDate.toLocalDateTime()));
    }

    description.append(DOT_MSG);

    return description.toString();
  }

  private ZonedDateTime getDateInTenantTimeZone(LocalDateTime localDateTime, ZoneId zoneId) {
    return  localDateTime.atZone(ZoneId.of(ZoneOffset.UTC.getId())).withZoneSameInstant(zoneId);
  }
}
