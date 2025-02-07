package org.folio.util.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.folio.CopilotGenerated;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@UnitTest
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
  void shouldExtractUserIdFromEvent() {
    var event = new InventoryEvent();
    var payload = new HashMap<String, Object>();
    var metadata = new HashMap<String, Object>();
    metadata.put("updatedByUserId", "user123");
    payload.put("metadata", metadata);
    event.setNewValue(payload);

    var userId = InventoryUtils.extractUserId(event);
    assertEquals("user123", userId);
  }

  @Test
  void shouldReturnDefaultIdWhenUserIdNotPresentInEvent() {
    var event = new InventoryEvent();
    var payload = new HashMap<String, Object>();
    event.setNewValue(payload);

    var userId = InventoryUtils.extractUserId(event);
    assertEquals(InventoryUtils.DEFAULT_ID, userId);
  }

  @Test
  void shouldExtractUserIdWhenBothUpdatedAndCreatedByUserIdPresent() {
    var event = new InventoryEvent();
    var payload = new HashMap<String, Object>();
    var metadata = new HashMap<String, Object>();
    metadata.put("updatedByUserId", "user123");
    metadata.put("createdByUserId", "user456");
    payload.put("metadata", metadata);
    event.setNewValue(payload);

    var userId = InventoryUtils.extractUserId(event);
    assertEquals("user123", userId);
  }

  @Test
  void shouldReturnDefaultIdWhenNeitherUpdatedNorCreatedByUserIdPresent() {
    var event = new InventoryEvent();
    var payload = new HashMap<String, Object>();
    var metadata = new HashMap<String, Object>();
    payload.put("metadata", metadata);
    event.setNewValue(payload);

    var userId = InventoryUtils.extractUserId(event);
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

  @Test
  void shouldGetMapValueByPathWithNestedMapsAndLists() {
    var payload = Map.<String, Object>of(
      "metadata", Map.of(
        "users", List.of(
          Map.of("id", "user1"),
          Map.of("id", "user2")
        )
      )
    );

    var value = InventoryUtils.getMapValueByPath("metadata.users.id", payload);
    assertEquals(List.of("user1", "user2"), value);
  }

  @ParameterizedTest
  @EnumSource(InventoryKafkaEvent.class)
  void shouldFormatInventoryTopicPattern(InventoryKafkaEvent eventType) {
    var pattern = InventoryUtils.formatInventoryTopicPattern("env", eventType);
    assertEquals("(env\\.)(.*\\.)" + eventType.getTopicPattern(), pattern);
  }
}
