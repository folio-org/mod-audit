package org.folio.builder.description;

import static java.lang.String.format;
import static org.folio.builder.description.Descriptions.CHECKED_OUT_TO_PROXY_MSG;
import static org.folio.util.JsonPropertyFetcher.getProperty;
import static org.folio.util.LogEventPayloadField.PROXY_BARCODE;

import java.util.Objects;

import io.vertx.core.json.JsonObject;

public class LoanCheckOutDescriptionBuilder implements DescriptionBuilder {
  @Override
  public String buildDescription(JsonObject logEventPayload) {
    String proxyBarcode = getProperty(logEventPayload, PROXY_BARCODE);
    return format(CHECKED_OUT_TO_PROXY_MSG, Objects.nonNull(proxyBarcode) ? proxyBarcode : "no");
  }
}
