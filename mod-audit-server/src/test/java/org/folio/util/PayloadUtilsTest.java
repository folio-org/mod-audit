package org.folio.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.folio.CopilotGenerated;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
@CopilotGenerated
class PayloadUtilsTest {

  @Test
  void shouldGetMapValueByPath() {
    var payload = new HashMap<String, Object>();
    var metadata = new HashMap<String, Object>();
    metadata.put("createdByUserId", "user123");
    payload.put("metadata", metadata);

    var value = PayloadUtils.getMapValueByPath("metadata.createdByUserId", payload);
    assertEquals("user123", value);
  }

  @Test
  void shouldReturnNullWhenPathNotPresent() {
    var payload = new HashMap<String, Object>();

    var value = PayloadUtils.getMapValueByPath("metadata.createdByUserId", payload);
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

    var value = PayloadUtils.getMapValueByPath("metadata.users.id", payload);
    assertEquals(List.of("user1", "user2"), value);
  }

  @Test
  void shouldReturnNullWhenMapIsNull() {
    var value = PayloadUtils.getMapValueByPath("metadata", null);
    assertNull(value);
  }

  @Test
  void shouldExtractUpdatedByUserId() {
    var metadata = new HashMap<String, Object>();
    metadata.put("updatedByUserId", "user123");
    var payload = Map.<String, Object>of("metadata", metadata);

    var userId = PayloadUtils.extractPerformedByUserId(payload);
    assertEquals("user123", userId);
  }

  @Test
  void shouldFallBackToCreatedByUserId() {
    var metadata = new HashMap<String, Object>();
    metadata.put("createdByUserId", "user456");
    var payload = Map.<String, Object>of("metadata", metadata);

    var userId = PayloadUtils.extractPerformedByUserId(payload);
    assertEquals("user456", userId);
  }

  @Test
  void shouldPreferUpdatedByOverCreatedBy() {
    var metadata = new HashMap<String, Object>();
    metadata.put("updatedByUserId", "user123");
    metadata.put("createdByUserId", "user456");
    var payload = Map.<String, Object>of("metadata", metadata);

    var userId = PayloadUtils.extractPerformedByUserId(payload);
    assertEquals("user123", userId);
  }

  @Test
  void shouldReturnNullWhenPayloadIsNull() {
    assertNull(PayloadUtils.extractPerformedByUserId(null));
  }

  @Test
  void shouldReturnNullWhenNoUserIdPresent() {
    var metadata = new HashMap<String, Object>();
    var payload = Map.<String, Object>of("metadata", metadata);

    assertNull(PayloadUtils.extractPerformedByUserId(payload));
  }

  @Test
  void shouldReturnNullWhenUserIdIsNotString() {
    var metadata = new HashMap<String, Object>();
    metadata.put("updatedByUserId", 12345);
    var payload = Map.<String, Object>of("metadata", metadata);

    assertNull(PayloadUtils.extractPerformedByUserId(payload));
  }
}
