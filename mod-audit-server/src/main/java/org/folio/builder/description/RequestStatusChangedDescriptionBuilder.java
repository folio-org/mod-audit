package org.folio.builder.description;

import static java.lang.String.format;
import static org.folio.builder.description.Descriptions.REQUEST_STATUS_CHANGED_MSG;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.NEW_REQUEST_STATUS;
import static org.folio.util.LogEventPayloadField.OLD_REQUEST_STATUS;
import static org.folio.util.LogEventPayloadField.PICK_UP_SERVICE_POINT;
import static org.folio.util.LogEventPayloadField.REQUEST_ADDRESS_TYPE;
import static org.folio.util.LogEventPayloadField.REQUEST_TYPE;

import org.folio.util.JsonPropertyFetcher;

import io.vertx.core.json.JsonObject;

import java.util.Optional;

public class RequestStatusChangedDescriptionBuilder implements DescriptionBuilder {
  @Override
  public String buildDescription(JsonObject logEventPayload) {

    StringBuilder description = new StringBuilder();

    Optional.ofNullable(getProperty(logEventPayload, REQUEST_TYPE))
      .ifPresent(type -> description.append("Type: ").append(type).append(". "));

    Optional.ofNullable(getProperty(logEventPayload, PICK_UP_SERVICE_POINT))
      .ifPresent(pickUpServicePoint -> description.append("Pickup service point: ").append(pickUpServicePoint).append(". "));

    Optional.ofNullable(getProperty(logEventPayload, REQUEST_ADDRESS_TYPE))
      .ifPresent(addressType -> description.append("Address type: ").append(addressType).append(". "));

    Optional.ofNullable(getProperty(logEventPayload, NEW_REQUEST_STATUS))
      .ifPresent(newRequestStatus -> description.append("New request status: ").append(newRequestStatus).append(" "));

    Optional.ofNullable(getProperty(logEventPayload, OLD_REQUEST_STATUS))
      .ifPresent(oldRequestStatus -> description.append("(from: ").append(oldRequestStatus).append(")."));

    return description.toString();
  }
}
