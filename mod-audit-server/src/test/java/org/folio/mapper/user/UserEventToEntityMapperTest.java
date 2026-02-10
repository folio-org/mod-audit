package org.folio.mapper.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import org.folio.services.diff.user.UserDiffCalculator;
import org.folio.util.user.UserEvent;
import org.folio.util.user.UserEventType;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@UnitTest
class UserEventToEntityMapperTest {

  private UserEventToEntityMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new UserEventToEntityMapper(new UserDiffCalculator());
  }

  @Test
  void shouldMapUpdatedEventWithDiff() {
    var event = createUserEvent(UserEventType.UPDATED);
    event.setOldValue(Map.of("username", "oldUser", "id", "123"));
    event.setNewValue(Map.of("username", "newUser", "id", "123",
      "metadata", Map.of("updatedByUserId", UUID.randomUUID().toString())));

    var result = mapper.apply(event);

    assertEquals(UUID.fromString(event.getId()), result.eventId());
    assertEquals(new Timestamp(event.getTimestamp()), result.eventDate());
    assertEquals(UUID.fromString(event.getUserId()), result.userId());
    assertEquals(UserEventType.UPDATED.name(), result.action());
    assertNotNull(result.diff());
    assertNotNull(result.performedBy());
  }

  @EnumSource(value = UserEventType.class, names = {"CREATED", "DELETED"})
  @ParameterizedTest
  void shouldMapCreateAndDeleteWithNullDiff(UserEventType eventType) {
    var event = createUserEvent(eventType);

    var result = mapper.apply(event);

    assertEquals(UUID.fromString(event.getId()), result.eventId());
    assertEquals(new Timestamp(event.getTimestamp()), result.eventDate());
    assertEquals(UUID.fromString(event.getUserId()), result.userId());
    assertEquals(eventType.name(), result.action());
    assertNull(result.diff());
  }

  @Test
  void shouldMapUpdateEventWithNullDiffIfNoChanges() {
    var event = createUserEvent(UserEventType.UPDATED);
    var sameData = Map.<String, Object>of("username", "sameUser", "id", "123");
    event.setOldValue(sameData);
    event.setNewValue(sameData);

    var result = mapper.apply(event);

    assertEquals(UserEventType.UPDATED.name(), result.action());
    assertNull(result.diff());
  }

  @Test
  void shouldHandleNullPerformedBy() {
    var event = createUserEvent(UserEventType.CREATED);
    event.setNewValue(Map.of("username", "testuser"));

    var result = mapper.apply(event);

    assertNull(result.performedBy());
  }

  private UserEvent createUserEvent(UserEventType type) {
    return UserEvent.builder()
      .id(UUID.randomUUID().toString())
      .type(type)
      .tenant("diku")
      .timestamp(System.currentTimeMillis())
      .userId(UUID.randomUUID().toString())
      .newValue(Map.of("key", "value"))
      .oldValue(Map.of("key", "oldValue"))
      .build();
  }
}
