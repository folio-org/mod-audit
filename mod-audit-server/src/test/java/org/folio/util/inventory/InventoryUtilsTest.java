package org.folio.util.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import org.folio.CopilotGenerated;
import org.junit.jupiter.api.Test;

@CopilotGenerated
class InventoryUtilsTest {

  @Test
  void shouldExtractEntityIdFromPayload() {
    var payload = new HashMap<String, Object>();
    payload.put("id", "12345");

    var entityId = InventoryUtils.extractEntityIdFromPayload(payload);
    assertEquals("12345", entityId);
  }

  @Test
  void shouldReturnDefaultIdWhenEntityIdNotPresent() {
    var payload = new HashMap<String, Object>();

    var entityId = InventoryUtils.extractEntityIdFromPayload(payload);
    assertEquals(InventoryUtils.DEFAULT_ID, entityId);
  }

  @Test
  void shouldExtractUserIdFromPayload() {
    var payload = new HashMap<String, Object>();
    var metadata = new HashMap<String, Object>();
    metadata.put("updatedByUserId", "user123");
    payload.put("metadata", metadata);

    var userId = InventoryUtils.extractUserId(payload);
    assertEquals("user123", userId);
  }

  @Test
  void shouldReturnDefaultIdWhenUserIdNotPresent() {
    var payload = new HashMap<String, Object>();

    var userId = InventoryUtils.extractUserId(payload);
    assertEquals(InventoryUtils.DEFAULT_ID, userId);
  }

  @Test
  void shouldGetEventPayload() {
    var event = new InventoryEvent();
    var newValue = new HashMap<String, Object>();
    newValue.put("key", "value");
    event.setNewValue(newValue);

    var payload = InventoryUtils.getEventPayload(event);
    assertEquals(newValue, payload);
  }

  @Test
  void shouldGetMapValueByPath() {
    var payload = new HashMap<String, Object>();
    var metadata = new HashMap<String, Object>();
    metadata.put("createdByUserId", "user123");
    payload.put("metadata", metadata);

    var value = InventoryUtils.getMapValueByPath("metadata.createdByUserId", payload);
    assertEquals("user123", value);
  }

  @Test
  void shouldReturnNullWhenPathNotPresent() {
    var payload = new HashMap<String, Object>();

    var value = InventoryUtils.getMapValueByPath("metadata.createdByUserId", payload);
    assertNull(value);
  }
}