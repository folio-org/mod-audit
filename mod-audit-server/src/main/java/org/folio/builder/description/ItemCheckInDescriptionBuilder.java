package org.folio.builder.description;

import static java.lang.String.format;
import static org.folio.builder.description.Descriptions.DOT_MSG;
import static org.folio.builder.description.Descriptions.ITEM_STATUS_MSG;
import static org.folio.builder.description.Descriptions.TO_MSG;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.DESTINATION_SERVICE_POINT;
import static org.folio.util.LogEventPayloadField.ITEM_STATUS_NAME;

import org.folio.builder.ItemStatus;

import io.vertx.core.json.JsonObject;

public class ItemCheckInDescriptionBuilder implements DescriptionBuilder {
  @Override
  public String buildDescription(JsonObject logEventPayload) {
    ItemStatus status = ItemStatus.from(getProperty(logEventPayload, ITEM_STATUS_NAME));

    StringBuilder description = new StringBuilder(format(ITEM_STATUS_MSG, status.getValue()));
    if (status == ItemStatus.IN_TRANSIT) {
      String destinationServicePoint = getProperty(logEventPayload, DESTINATION_SERVICE_POINT);
      description.append(format(TO_MSG, destinationServicePoint));
    }
    description.append(DOT_MSG);
    return description.toString();
  }
}
