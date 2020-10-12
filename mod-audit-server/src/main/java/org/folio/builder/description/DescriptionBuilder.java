package org.folio.builder.description;

import io.vertx.core.json.JsonObject;

public interface DescriptionBuilder {
  String buildDescription(JsonObject logEventPayload);
}
