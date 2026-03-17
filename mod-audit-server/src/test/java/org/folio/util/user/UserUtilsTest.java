package org.folio.util.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class UserUtilsTest {

  private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";

  @Test
  void shouldExtractPerformedByFromUpdatedByUserId() {
    var event = UserEvent.builder()
      .newValue(Map.of("metadata", Map.of("updatedByUserId", USER_ID)))
      .build();

    assertEquals(USER_ID, UserUtils.extractPerformedBy(event));
  }

  @Test
  void shouldExtractPerformedByFromCreatedByUserId() {
    var event = UserEvent.builder()
      .newValue(Map.of("metadata", Map.of("createdByUserId", USER_ID)))
      .build();

    assertEquals(USER_ID, UserUtils.extractPerformedBy(event));
  }

  @Test
  void shouldPreferUpdatedByOverCreatedBy() {
    var updatedBy = "660e8400-e29b-41d4-a716-446655440001";
    var event = UserEvent.builder()
      .newValue(Map.of("metadata", Map.of("updatedByUserId", updatedBy, "createdByUserId", USER_ID)))
      .build();

    assertEquals(updatedBy, UserUtils.extractPerformedBy(event));
  }

  @Test
  void shouldReturnNullWhenNoMetadata() {
    var event = UserEvent.builder()
      .newValue(Map.of("username", "testuser"))
      .build();

    assertNull(UserUtils.extractPerformedBy(event));
  }

  @Test
  void shouldReturnNullWhenNoPayload() {
    var event = UserEvent.builder().build();

    assertNull(UserUtils.extractPerformedBy(event));
  }

  @Test
  void shouldFallbackToOldValueWhenNewValueIsNull() {
    var event = UserEvent.builder()
      .oldValue(Map.of("metadata", Map.of("createdByUserId", USER_ID)))
      .build();

    assertEquals(USER_ID, UserUtils.extractPerformedBy(event));
  }

  @Test
  void shouldFormatUserTopicPattern() {
    var pattern = UserUtils.formatUserTopicPattern("folio", UserKafkaEvent.USER);

    assertNotNull(pattern);
    assertEquals("(folio\\.)(.*\\.)users\\.users", pattern);
  }
}
