package org.folio.util;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.joda.time.DateTime.parse;

import org.joda.time.DateTime;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class JsonPropertyFetcher {

  private JsonPropertyFetcher() {
  }

  public static JsonObject getObjectProperty(JsonObject representation, LogEventPayloadField field) {
    if (representation != null) {
      return representation.getJsonObject(field.value());
    } else {
      return null;
    }
  }

  public static JsonObject getNestedObjectProperty(JsonObject representation, LogEventPayloadField object,
                                                   LogEventPayloadField property) {

    if (representation == null) {
      return null;
    }

    if (representation.containsKey(object.value())) {
      final JsonObject obj = representation.getJsonObject(object.value());

      return getObjectProperty(obj, property);
    } else {
      return null;
    }
  }

  public static DateTime getDateTimeProperty(JsonObject representation, LogEventPayloadField field) {
    return getDateTimeProperty(representation, field, null);
  }

  public static DateTime getDateTimeProperty(JsonObject representation, LogEventPayloadField field, DateTime defaultValue) {
    if (representation != null && isNotBlank(representation.getString(field.value()))) {
      return parse(representation.getString(field.value()));
    } else {
      return defaultValue;
    }
  }

  public static JsonArray getArrayProperty(JsonObject representation, LogEventPayloadField field) {
    if (representation == null) {
      return new JsonArray();
    }
    JsonArray val = representation.getJsonArray(field.value());
    return val != null ? val : new JsonArray();
  }

  public static String getProperty(JsonObject representation, LogEventPayloadField field) {
    if (representation != null) {
      return representation.getString(field.value());
    } else {
      return null;
    }
  }

  public static boolean getBooleanProperty(JsonObject representation, LogEventPayloadField field) {
    if (representation != null) {
      return representation.getBoolean(field.value(), false);
    } else {
      return false;
    }
  }

  public static String getNestedStringProperty(JsonObject representation,
                                               LogEventPayloadField object, LogEventPayloadField field) {

    if (representation == null) {
      return null;
    }

    return representation.containsKey(object.value())
      ? representation.getJsonObject(object.value()).getString(field.value())
      : null;
  }
}
