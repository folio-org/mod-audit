package org.folio.util.inventory;

import static org.apache.commons.collections4.MapUtils.getString;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

@UtilityClass
public class InventoryUtils {

  public static final String DEFAULT_ID = "00000000-0000-0000-0000-000000000000";

  private static final String CONSORTIUM_SOURCE = "CONSORTIUM-";

  private static final String SOURCE_FIELD = "source";
  private static final String UPDATED_BY_USER_ID_PATH = "metadata.updatedByUserId";
  private static final String CREATED_BY_USER_ID_PATH = "metadata.createdByUserId";

  public static String extractUserId(InventoryEvent event) {
    var payload = getEventPayload(event);
    var value = Optional.ofNullable(getMapValueByPath(UPDATED_BY_USER_ID_PATH, payload))
      .or(() -> Optional.ofNullable(getMapValueByPath(CREATED_BY_USER_ID_PATH, payload)))
      .orElse(DEFAULT_ID);
    return value instanceof String id ? id : DEFAULT_ID;
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> getEventPayload(InventoryEvent event) {
    return event.getNewValue() != null
      ? event.getNewValue()
      : event.getOldValue();
  }

  /**
   * Retrieves field value by path. It will extract all field value matching following keys separated by dot. If map
   * value contain list of maps - it will extract all related values from each sub map.
   *
   * @param path path in format 'field1.field2.field3'
   * @param map  map to process
   * @return field value by path.
   */
  public static Object getMapValueByPath(String path, Map<String, Object> map) {
    if (MapUtils.isEmpty(map)) {
      return null;
    }
    var values = path.split("\\.");
    Object currentValue = map;
    for (String pathValue : values) {
      currentValue = getFieldValueByPath(pathValue, currentValue);
      if (currentValue == null) {
        break;
      }
    }
    return currentValue;
  }

  public static String formatInventoryTopicPattern(String env, InventoryKafkaEvent eventType) {
    return "(%s\\.)(.*\\.)%s".formatted(env, eventType.getTopicPattern());
  }

  @SuppressWarnings("unchecked")
  private static Object getFieldValueByPath(String pathValue, Object value) {
    if (value instanceof Map) {
      return ((Map<String, Object>) value).get(pathValue);
    }
    if (value instanceof List) {
      var result = ((List<Object>) value).stream()
        .map(listValue -> getFieldValueByPath(pathValue, listValue))
        .filter(Objects::nonNull)
        .toList();
      return CollectionUtils.isNotEmpty(result) ? result : null;
    }
    return null;
  }

  public static boolean isShadowCopyEvent(InventoryEvent event) {
    var payload = getEventPayload(event);
    var source = getString(payload, SOURCE_FIELD);
    return source != null && source.startsWith(CONSORTIUM_SOURCE);
  }
}
