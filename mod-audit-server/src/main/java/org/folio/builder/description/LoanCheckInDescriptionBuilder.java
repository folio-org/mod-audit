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

import java.time.LocalDateTime;
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

    LocalDateTime returnDate = getDateTimeProperty(logEventPayload, RETURN_DATE);
    LocalDateTime systemReturnDate = getDateTimeProperty(logEventPayload, SYSTEM_RETURN_DATE);
    LocalDateTime dueDate = getDateTimeProperty(logEventPayload, DUE_DATE);

    if (!returnDate.isEqual(systemReturnDate)) {
      description.append(BACKDATED_TO_MSG).append(getFormattedDateTime(returnDate));
    }

    if (dueDate.isAfter(returnDate)) {
      description.append(OVERDUE_DUE_DATE_MSG).append(getFormattedDateTime(dueDate));
    }

    description.append(DOT_MSG);

    return description.toString();
  }
}
