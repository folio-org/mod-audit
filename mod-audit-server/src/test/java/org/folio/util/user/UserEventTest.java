package org.folio.util.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class UserEventTest {

  @Test
  void shouldExtractOldAndNewFromData() {
    var oldValue = Map.<String, Object>of("username", "oldUser");
    var newValue = Map.<String, Object>of("username", "newUser");
    var data = Map.<String, Object>of("old", oldValue, "new", newValue);

    var event = new UserEvent();
    event.setData(data);

    assertEquals(oldValue, event.getOldValue());
    assertEquals(newValue, event.getNewValue());
  }

  @Test
  void shouldHandleNullData() {
    var event = new UserEvent();
    event.setData(null);

    assertNull(event.getOldValue());
    assertNull(event.getNewValue());
  }

  @Test
  void shouldHandleMissingOldInData() {
    var newValue = Map.<String, Object>of("username", "newUser");
    var data = Map.<String, Object>of("new", newValue);

    var event = new UserEvent();
    event.setData(data);

    assertNull(event.getOldValue());
    assertEquals(newValue, event.getNewValue());
  }

  @Test
  void shouldHandleMissingNewInData() {
    var oldValue = Map.<String, Object>of("username", "oldUser");
    var data = Map.<String, Object>of("old", oldValue);

    var event = new UserEvent();
    event.setData(data);

    assertEquals(oldValue, event.getOldValue());
    assertNull(event.getNewValue());
  }
}
