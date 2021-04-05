package org.folio.builder.description;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.builder.description.DescriptionHelper.getFormattedDateTime;

import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.manualblock.ManualBlock;

public class ManualBlockDescriptionBuilder {

  public String buildDescription(ManualBlock block) {

    var description = new StringBuilder();

    populateBlockActionDescription(block, description);

    isPropertyPresented(block.getDesc(), desc -> appendDescriptionPart(description, "Description: ", desc));
    isPropertyPresented(block.getStaffInformation(), info -> appendDescriptionPart(description, "Staff only information: ", info));
    isPropertyPresented(block.getPatronMessage(), msg -> appendDescriptionPart(description, "Message to patron: ", msg));
    isPropertyPresented(getFormattedDateTime(block.getExpirationDate()), date -> appendDescriptionPart(description, "Expiration date: ", date));

    return description.toString()
      .trim();
  }

  private void appendDescriptionPart(StringBuilder description, String key, String value) {
    description.append(key)
      .append(value)
      .append(". ");
  }

  private void isPropertyPresented(String value, Consumer<String> consumer) {
    if (StringUtils.isNotEmpty(value) && StringUtils.isNotBlank(value)) {
      consumer.accept(value);
    }
  }

  private void populateBlockActionDescription(ManualBlock block, StringBuilder description) {
    StringBuilder actions = new StringBuilder();
    if (block.getBorrowing()) {
      actions.append("borrowing, ");
    }
    if (block.getRenewals()) {
      actions.append("renewals, ");
    }
    if (block.getRequests()) {
      actions.append("requests, ");
    }
    if (isNotEmpty(actions)) {
      actions.replace(actions.length() - 2, actions.length(), ". ");
      description.append("Block actions: ")
        .append(actions);
    }
  }
}
