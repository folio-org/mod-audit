package org.folio.builder.description;

import io.vertx.core.json.JsonObject;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.builder.description.Descriptions.BILLED_MSG;
import static org.folio.builder.description.Descriptions.CANCELLED_MSG;
import static org.folio.builder.description.Descriptions.FEE_FINE_PAID_MSG;
import static org.folio.builder.description.Descriptions.FEE_FINE_REFUND_MSG;
import static org.folio.builder.description.Descriptions.FEE_FINE_TRANSFERRED_MSG;
import static org.folio.builder.description.Descriptions.FEE_FINE_WAIVED_MSG;
import static org.folio.util.JsonPropertyFetcher.getBooleanProperty;
import static org.folio.util.JsonPropertyFetcher.getDoubleProperty;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import org.folio.util.LogEventPayloadField;
import static org.folio.util.LogEventPayloadField.AMOUNT;
import static org.folio.util.LogEventPayloadField.AUTOMATED;
import static org.folio.util.LogEventPayloadField.BALANCE;
import static org.folio.util.LogEventPayloadField.COMMENTS;
import static org.folio.util.LogEventPayloadField.FEE_FINE_OWNER;
import static org.folio.util.LogEventPayloadField.PAYMENT_METHOD;
import static org.folio.util.LogEventPayloadField.TYPE;

import java.util.Arrays;
import java.util.stream.Collectors;

public class FeeFineDescriptionBuilder implements DescriptionBuilder {
  private static final String BILLED = "Billed";
  private static final String PAID_FULLY = "Paid fully";
  private static final String PAID_PARTIALLY = "Paid partially";
  private static final String WAIVED_FULLY = "Waived fully";
  private static final String WAIVED_PARTIALLY = "Waived partially";
  private static final String REFUNDED_FULLY = "Refunded fully";
  private static final String REFUNDED_PARTIALLY = "Refunded partially";
  private static final String TRANSFERRED_FULLY = "Transferred fully";
  private static final String TRANSFERRED_PARTIALLY = "Transferred partially";
  private static final String STAFF_INFO_ONLY = "Staff information only added";
  private static final String CANCELLED = "Cancelled as error";
  private static final String STAFF = "STAFF : ";
  private static final String PATRON = "PATRON : ";

  @Override
  public String buildDescription(JsonObject logEventPayload) {
    switch (getProperty(logEventPayload, LogEventPayloadField.ACTION)) {
      case BILLED:
        return String.format(BILLED_MSG,
          getProperty(logEventPayload, TYPE),
          getProperty(logEventPayload, FEE_FINE_OWNER),
          getDoubleProperty(logEventPayload, AMOUNT),
          getBooleanProperty(logEventPayload, AUTOMATED) ? "automated" : "manual",
          getProperty(logEventPayload, COMMENTS));
      case PAID_FULLY:
      case PAID_PARTIALLY:
        return createDescriptionFor(FEE_FINE_PAID_MSG, logEventPayload);
      case WAIVED_FULLY:
      case WAIVED_PARTIALLY:
        return createDescriptionFor(FEE_FINE_WAIVED_MSG, logEventPayload);
      case REFUNDED_FULLY:
      case REFUNDED_PARTIALLY:
        return createDescriptionFor(FEE_FINE_REFUND_MSG, logEventPayload);
      case TRANSFERRED_FULLY:
      case TRANSFERRED_PARTIALLY:
        return createDescriptionFor(FEE_FINE_TRANSFERRED_MSG, logEventPayload);
      case CANCELLED:
        return String.format(CANCELLED_MSG,
          getDoubleProperty(logEventPayload, AMOUNT),
          extractInfo(getProperty(logEventPayload, COMMENTS), STAFF),
          extractInfo(getProperty(logEventPayload, COMMENTS), PATRON));
      case STAFF_INFO_ONLY:
        return extractInfo(getProperty(logEventPayload, COMMENTS), STAFF);
      default:
        return EMPTY;
    }
  }

  private String createDescriptionFor(String template, JsonObject logEventPayload) {
    return String.format(template,
      getProperty(logEventPayload, TYPE),
      getDoubleProperty(logEventPayload, AMOUNT),
      getDoubleProperty(logEventPayload, BALANCE),
      getProperty(logEventPayload, PAYMENT_METHOD),
      extractInfo(getProperty(logEventPayload, COMMENTS), STAFF),
      extractInfo(getProperty(logEventPayload, COMMENTS), PATRON));
  }

  private String extractInfo(String comment, String forWhom) {
    if (nonNull(comment)) {
      String[] tokens = comment.split("\n");
      return Arrays.stream(tokens)
        .map(String::trim)
        .filter(s -> s.contains(forWhom))
        .map(s -> s.replace(forWhom, EMPTY))
        .collect(Collectors.joining());
    }
    return EMPTY;
  }
}
