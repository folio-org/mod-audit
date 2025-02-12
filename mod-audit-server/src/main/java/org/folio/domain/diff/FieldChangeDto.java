package org.folio.domain.diff;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldChangeDto {

  private ChangeType changeType;
  private String fieldName;
  private String fullPath;
  private Object oldValue;
  private Object newValue;

  public static FieldChangeDto of(String fieldName, String fullPath, Object oldValue, Object newValue) {
    if (oldValue == null) {
      return added(fieldName, fullPath, newValue);
    } else if (newValue == null) {
      return removed(fieldName, fullPath, oldValue);
    } else if (oldValue.equals(newValue)) {
      return nothing(fieldName, fullPath, oldValue);
    } else {
      return modified(fieldName, fullPath, oldValue, newValue);
    }
  }

  public static FieldChangeDto modified(String fieldName, String fullPath, Object oldValue, Object newValue) {
    return new FieldChangeDto(ChangeType.MODIFIED, fieldName, fullPath, oldValue, newValue);
  }

  public static FieldChangeDto added(String fieldName, String fullPath, Object newValue) {
    return new FieldChangeDto(ChangeType.ADDED, fieldName, fullPath, null, newValue);
  }

  public static FieldChangeDto removed(String fieldName, String fullPath, Object oldValue) {
    return new FieldChangeDto(ChangeType.REMOVED, fieldName, fullPath, oldValue, null);
  }

  public static FieldChangeDto nothing(String fieldName, String fullPath, Object value) {
    return new FieldChangeDto(ChangeType.NOTHING, fieldName, fullPath, value, value);
  }
}
