package org.folio.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import org.folio.util.inventory.InventoryEvent;
import org.folio.util.inventory.InventoryEventType;
import org.folio.util.inventory.InventoryUtils;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

@UnitTest
class InventoryEventToEntityMapperTest {

  private InventoryEventToEntityMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new InventoryEventToEntityMapper();
  }

  @Test
  void shouldMapInventoryEventToInventoryAuditEntity() {
    var event = new InventoryEvent();
    event.setEventId(UUID.randomUUID().toString());
    event.setEntityId(UUID.randomUUID().toString());
    event.setEventTs(System.currentTimeMillis());
    event.setType(InventoryEventType.CREATE);
    event.setNewValue(Map.of("key", "value"));

    try (MockedStatic<InventoryUtils> utilities = mockStatic(InventoryUtils.class)) {
      utilities.when(() -> InventoryUtils.extractUserId(event)).thenReturn(UUID.randomUUID().toString());

      var result = mapper.apply(event);

      assertEquals(UUID.fromString(event.getEventId()), result.eventId());
      assertEquals(new Timestamp(event.getEventTs()), result.eventDate());
      assertEquals(UUID.fromString(event.getEntityId()), result.entityId());
      assertEquals(InventoryEventType.CREATE.name(), result.action());
      assertEquals(UUID.fromString(InventoryUtils.extractUserId(event)), result.userId());
      assertEquals(event.getNewValue(), result.diff());
    }
  }

  @Test
  void shouldMapDeleteEventWithNullDiff() {
    var event = new InventoryEvent();
    event.setEventId(UUID.randomUUID().toString());
    event.setEntityId(UUID.randomUUID().toString());
    event.setEventTs(System.currentTimeMillis());
    event.setType(InventoryEventType.DELETE);

    try (MockedStatic<InventoryUtils> utilities = mockStatic(InventoryUtils.class)) {
      utilities.when(() -> InventoryUtils.extractUserId(event)).thenReturn(UUID.randomUUID().toString());

      var result = mapper.apply(event);

      assertEquals(UUID.fromString(event.getEventId()), result.eventId());
      assertEquals(new Timestamp(event.getEventTs()), result.eventDate());
      assertEquals(UUID.fromString(event.getEntityId()), result.entityId());
      assertEquals(InventoryEventType.DELETE.name(), result.action());
      assertEquals(UUID.fromString(InventoryUtils.extractUserId(event)), result.userId());
      assertNull(result.diff());
    }
  }
}