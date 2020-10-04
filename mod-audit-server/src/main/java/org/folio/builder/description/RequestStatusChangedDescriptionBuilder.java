package org.folio.builder.description;

import static java.lang.String.format;
import static org.folio.builder.description.Descriptions.REQUEST_STATUS_CHANGED_MSG;
import static org.folio.util.LogEventPayloadField.NEW_REQUEST_STATUS;
import static org.folio.util.LogEventPayloadField.OLD_REQUEST_STATUS;
import static org.folio.util.LogEventPayloadField.PICK_UP_SERVICE_POINT;
import static org.folio.util.LogEventPayloadField.REQUEST_ADDRESS_TYPE;
import static org.folio.util.LogEventPayloadField.REQUEST_TYPE;

import org.folio.util.JsonPropertyFetcher;

import io.vertx.core.json.JsonObject;

public class RequestStatusChangedDescriptionBuilder implements DescriptionBuilder {
  @Override
  public String buildDescription(JsonObject logEventPayload) {
    String requestType = JsonPropertyFetcher.getProperty(logEventPayload, REQUEST_TYPE);
    String oldRequestStatus = JsonPropertyFetcher.getProperty(logEventPayload, OLD_REQUEST_STATUS);
    String newRequestStatus = JsonPropertyFetcher.getProperty(logEventPayload, NEW_REQUEST_STATUS);
    String pickupServicePoint = JsonPropertyFetcher.getProperty(logEventPayload, PICK_UP_SERVICE_POINT);
    String addressType = JsonPropertyFetcher.getProperty(logEventPayload, REQUEST_ADDRESS_TYPE);
    return format(REQUEST_STATUS_CHANGED_MSG, requestType, pickupServicePoint, addressType, newRequestStatus, oldRequestStatus);
  }
}
