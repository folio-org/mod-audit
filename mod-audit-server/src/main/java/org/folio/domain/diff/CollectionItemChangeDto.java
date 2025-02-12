package org.folio.domain.diff;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionItemChangeDto {

  private ChangeType changeType;
  private Object oldValue;
  private Object newValue;

  public static CollectionItemChangeDto of(ChangeType changeType, Object oldValue, Object newValue) {
    return new CollectionItemChangeDto(changeType, oldValue, newValue);
  }

  public static CollectionItemChangeDto of(Object oldValue, Object newValue) {
    if (oldValue == null) {
      return added( newValue);
    } else if (newValue == null) {
      return removed(oldValue);
    } else if (oldValue.equals(newValue)) {
      return nothing(oldValue);
    } else {
      return modified(oldValue, newValue);
    }
  }

  public static CollectionItemChangeDto added(Object newValue) {
    return new CollectionItemChangeDto(ChangeType.ADDED, null, newValue);
  }

  public static CollectionItemChangeDto removed(Object oldValue) {
    return new CollectionItemChangeDto(ChangeType.REMOVED, oldValue, null);
  }

  public static CollectionItemChangeDto nothing(Object value) {
    return new CollectionItemChangeDto(ChangeType.NOTHING, value, value);
  }

  public static CollectionItemChangeDto modified(Object oldValue, Object newValue) {
    return new CollectionItemChangeDto(ChangeType.MODIFIED, oldValue, newValue);
  }
}
