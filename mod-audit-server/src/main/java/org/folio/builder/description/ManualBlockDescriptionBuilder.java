package org.folio.builder.description;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.builder.description.DescriptionHelper.getFormattedDateTime;
import static org.folio.util.JsonPropertyFetcher.getBooleanProperty;
import static org.folio.util.JsonPropertyFetcher.getDateTimeProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.MANUAL_BLOCK_BORROWING;
import static org.folio.util.LogEventPayloadField.MANUAL_BLOCK_DESCRIPTION;
import static org.folio.util.LogEventPayloadField.MANUAL_BLOCK_EXPIRATION_DATE;
import static org.folio.util.LogEventPayloadField.MANUAL_BLOCK_MESSAGE_TO_PATRON;
import static org.folio.util.LogEventPayloadField.MANUAL_BLOCK_RENEWALS;
import static org.folio.util.LogEventPayloadField.MANUAL_BLOCK_REQUESTS;
import static org.folio.util.LogEventPayloadField.MANUAL_BLOCK_STAFF_INFORMATION;

import java.util.Optional;

import io.vertx.core.json.JsonObject;

public class ManualBlockDescriptionBuilder implements DescriptionBuilder {

  @Override
  public String buildDescription(JsonObject logEventPayload) {

    StringBuilder description = new StringBuilder();

    populateBlockActionsDescription(logEventPayload, description);

    Optional.ofNullable(getProperty(logEventPayload, MANUAL_BLOCK_DESCRIPTION))
      .ifPresent(desc -> description.append("Description: ")
        .append(desc)
        .append(". "));

    Optional.ofNullable(getProperty(logEventPayload, MANUAL_BLOCK_STAFF_INFORMATION))
      .ifPresent(staffInfo -> description.append("Staff only information: ")
        .append(staffInfo)
        .append(". "));

    Optional.ofNullable(getProperty(logEventPayload, MANUAL_BLOCK_MESSAGE_TO_PATRON))
      .ifPresent(msgToPatron -> description.append("Message to patron: ")
        .append(msgToPatron)
        .append(". "));

    Optional.ofNullable(getDateTimeProperty(logEventPayload, MANUAL_BLOCK_EXPIRATION_DATE))
      .ifPresent(expDate -> description.append("Expiration date: ")
        .append(getFormattedDateTime(expDate))
        .append(". "));

    return description.toString()
      .trim();
  }

  private void populateBlockActionsDescription(JsonObject logEventPayload, StringBuilder description) {
    StringBuilder actions = new StringBuilder();
    if (getBooleanProperty(logEventPayload, MANUAL_BLOCK_BORROWING)) {
      actions.append("borrowing, ");
    }
    if (getBooleanProperty(logEventPayload, MANUAL_BLOCK_RENEWALS)) {
      actions.append("renewals, ");
    }
    if (getBooleanProperty(logEventPayload, MANUAL_BLOCK_REQUESTS)) {
      actions.append("requests, ");
    }
    if (isNotEmpty(actions)) {
      actions.replace(actions.length() - 2, actions.length(), ". ");
      description.append("Block actions: ")
        .append(actions);
    }
  }
}
