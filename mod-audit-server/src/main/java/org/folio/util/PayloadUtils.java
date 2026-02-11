package org.folio.util;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

@UtilityClass
public class PayloadUtils {

  private static final String UPDATED_BY_USER_ID_PATH = "metadata.updatedByUserId";
  private static final String CREATED_BY_USER_ID_PATH = "metadata.createdByUserId";

  /**
   * Extracts the user ID of who performed the action from the event payload metadata.
   * Checks updatedByUserId first, then falls back to createdByUserId.
   *
   * @param payload the event payload map
   * @return the user ID string, or null if not found
   */
  public static String extractPerformedByUserId(Map<String, Object> payload) {
    if (payload == null) {
      return null;
    }
    var value = Optional.ofNullable(getMapValueByPath(UPDATED_BY_USER_ID_PATH, payload))
      .or(() -> Optional.ofNullable(getMapValueByPath(CREATED_BY_USER_ID_PATH, payload)))
      .orElse(null);
    return value instanceof String id ? id : null;
  }

  /**
   * Retrieves field value by dot-separated path. If map value contains a list of maps,
   * it will extract all related values from each sub map.
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
}
