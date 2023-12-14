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
import java.util.Comparator;

import org.folio.builder.ItemStatus;

import io.vertx.core.json.JsonObject;

public class LoanCheckInDescriptionBuilder implements DescriptionBuilder {
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

    ZoneId zoneId = ZoneId.of(getProperty(logEventPayload, ZONE_ID) != null ? getProperty(logEventPayload, ZONE_ID) : ZoneOffset.UTC.getId());
    ZonedDateTime returnDate = getDateInTenantTimeZone(getDateTimeProperty(logEventPayload, RETURN_DATE), zoneId);
    ZonedDateTime systemReturnDate = getDateInTenantTimeZone(getDateTimeProperty(logEventPayload, SYSTEM_RETURN_DATE), zoneId);
    LocalDateTime dueDate = getDateTimeProperty(logEventPayload, DUE_DATE);

    Comparator<ZonedDateTime> comparator = Comparator.comparing(
      zdt -> zdt.withSecond(0).withNano(0));

    if (comparator.compare(returnDate, systemReturnDate) != 0 ) {
      description.append(BACKDATED_TO_MSG).append(getFormattedDateTime(returnDate.toLocalDateTime()));
    }

    if (dueDate.isAfter(returnDate.toLocalDateTime())) {
      description.append(OVERDUE_DUE_DATE_MSG).append(getFormattedDateTime(dueDate));
    }

    description.append(DOT_MSG);

    return description.toString();
  }

  private ZonedDateTime getDateInTenantTimeZone(LocalDateTime localDateTime, ZoneId zoneId) {
    return  localDateTime.atZone(ZoneId.of(ZoneOffset.UTC.getId())).withZoneSameInstant(zoneId);
  }
}
