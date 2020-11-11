package org.folio.builder.description;

import static java.util.Objects.isNull;
import static org.folio.builder.description.DescriptionHelper.getFormattedDateTime;
import static org.folio.util.JsonPropertyFetcher.getDateTimeProperty;
import static org.folio.util.JsonPropertyFetcher.getIntegerProperty;
import static org.folio.util.JsonPropertyFetcher.getNestedStringProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.BARCODE;
import static org.folio.util.LogEventPayloadField.ITEM;
import static org.folio.util.LogEventPayloadField.REQUEST_ADDRESS_TYPE;
import static org.folio.util.LogEventPayloadField.REQUEST_EXPIRATION_DATE;
import static org.folio.util.LogEventPayloadField.REQUEST_FULFILMENT_PREFERENCE;
import static org.folio.util.LogEventPayloadField.REQUEST_PICKUP_SERVICE_POINT;
import static org.folio.util.LogEventPayloadField.REQUEST_POSITION;
import static org.folio.util.LogEventPayloadField.REQUEST_PREVIOUS_POSITION;
import static org.folio.util.LogEventPayloadField.REQUEST_SERVICE_POINT;
import static org.folio.util.LogEventPayloadField.REQUEST_TYPE;

import java.util.Objects;
import java.util.Optional;

import org.joda.time.DateTime;

import io.vertx.core.json.JsonObject;

public class RequestDescriptionBuilder {

  public String buildCreateDescription(JsonObject request) {

    StringBuilder description = buildBaseDescription(request);

    return description.toString()
      .trim();
  }

  public String buildEditedDescription(JsonObject original, JsonObject updated) {

    StringBuilder description = buildBaseDescription(original);

    String oldRequestType = getProperty(original, REQUEST_TYPE);
    String newRequestType = getProperty(updated, REQUEST_TYPE);

    if (!Objects.equals(oldRequestType, newRequestType)) {

      Optional.ofNullable(newRequestType)
        .ifPresent(s -> description.append("New request type: ")
          .append(s)
          .append(" "));

      Optional.ofNullable(oldRequestType)
        .ifPresent(s -> description.append("(from: ")
          .append(s)
          .append(")."));
    }

    DateTime oldRequestExpDate = getDateTimeProperty(original, REQUEST_EXPIRATION_DATE);
    DateTime newRequestExpDate = getDateTimeProperty(updated, REQUEST_EXPIRATION_DATE);

    if (isNeedToBeDescribed(newRequestExpDate, oldRequestExpDate)) {
      description.append("New expiration date: ")
        .append(getFormattedDateTime(newRequestExpDate))
        .append(" ")
        .append("(from: ")
        .append(getFormattedDateTime(oldRequestExpDate))
        .append("). ");
    }

    String oldFulfilmentPreference = getProperty(original, REQUEST_FULFILMENT_PREFERENCE);
    String newFulfilmentPreference = getProperty(updated, REQUEST_FULFILMENT_PREFERENCE);

    if (isNeedToBeDescribed(oldFulfilmentPreference, newFulfilmentPreference)) {
      description.append("New fulfilment preference: ")
        .append(newFulfilmentPreference)
        .append(" ")
        .append("(from: ")
        .append(oldFulfilmentPreference)
        .append("). ");
    }

    String oldPickUpServicePoint = getProperty(original, REQUEST_PICKUP_SERVICE_POINT);
    String newPickUpServicePoint = getProperty(updated, REQUEST_PICKUP_SERVICE_POINT);

    if (isNeedToBeDescribed(oldPickUpServicePoint, newPickUpServicePoint)) {
      description.append("New pickup service point: ")
        .append(newPickUpServicePoint)
        .append(" ")
        .append("(from: ")
        .append(oldPickUpServicePoint)
        .append("). ");
    }

    String oldAddressType = getProperty(original, REQUEST_ADDRESS_TYPE);
    String newAddressType = getProperty(updated, REQUEST_ADDRESS_TYPE);

    if (isNeedToBeDescribed(newAddressType, oldAddressType)) {
      description.append("New address type: ")
        .append(newAddressType)
        .append(" ")
        .append("(from: ")
        .append(oldAddressType)
        .append("). ");
    }

    return description.toString()
      .trim();
  }

  public String buildCancelledDescription(JsonObject original, String reasonForCancellation) {

    StringBuilder description = buildBaseDescription(original);

    description.append("Reason for cancellation: ")
        .append(reasonForCancellation)
        .append(".");

    return description.toString()
      .trim();
  }

  public String buildMovedDescription(JsonObject original, JsonObject updated) {

    StringBuilder description = buildBaseDescription(original);

    Optional.ofNullable(getNestedStringProperty(updated, ITEM, BARCODE))
      .ifPresent(newItemBarcode -> description.append("New item barcode: ")
        .append(newItemBarcode)
        .append(" "));

    Optional.ofNullable(getNestedStringProperty(original, ITEM, BARCODE))
      .ifPresent(oldItemBarcode -> description.append("(from: ")
        .append(oldItemBarcode)
        .append(")."));

    return description.toString()
      .trim();
  }

  public String buildReorderedDescription(JsonObject reordered) {

    StringBuilder description = buildBaseDescription(reordered);

    Optional.ofNullable(getIntegerProperty(reordered, REQUEST_POSITION, null))
      .ifPresent(position -> description.append("New queue position: ")
        .append(position)
        .append(" "));

    Optional.ofNullable(getIntegerProperty(reordered, REQUEST_PREVIOUS_POSITION, null))
      .ifPresent(previousPosition -> description.append("(from: ")
        .append(previousPosition)
        .append(")."));

    return description.toString()
      .trim();
  }

  private StringBuilder buildBaseDescription(JsonObject request) {
    StringBuilder description = new StringBuilder();

    Optional.ofNullable(getProperty(request, REQUEST_TYPE))
      .ifPresent(type -> description.append("Type: ")
        .append(type)
        .append(". "));

    Optional.ofNullable(getProperty(request, REQUEST_SERVICE_POINT))
      .ifPresent(servicePoint -> description.append("Service point: ")
        .append(servicePoint)
        .append(". "));

    Optional.ofNullable(getProperty(request, REQUEST_ADDRESS_TYPE))
      .ifPresent(addressType -> description.append("Address type: ")
        .append(addressType)
        .append(". "));

    return description;
  }

  private boolean isNeedToBeDescribed(String str1, String str2) {
    return !(isNull(str1) & isNull(str2)) && !str1.equals(str2);
  }

  private boolean isNeedToBeDescribed(DateTime dateTime1, DateTime dateTime2) {
    return !(isNull(dateTime1) & isNull(dateTime2)) && !dateTime1.isEqual(dateTime2);
  }
}
