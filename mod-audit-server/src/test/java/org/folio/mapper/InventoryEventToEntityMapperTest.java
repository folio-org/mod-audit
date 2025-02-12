package org.folio.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.services.diff.InstanceDiffCalculator;
import org.folio.util.inventory.InventoryEvent;
import org.folio.util.inventory.InventoryEventType;
import org.folio.util.inventory.InventoryResourceType;
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
    mapper = new InventoryEventToEntityMapper(List.of(new InstanceDiffCalculator()));
  }

  @Test
  void shouldMapInventoryEventToInventoryAuditEntity() {
    var event = new InventoryEvent();
    event.setEventId(UUID.randomUUID().toString());
    event.setEntityId(UUID.randomUUID().toString());
    event.setEventTs(System.currentTimeMillis());
    event.setType(InventoryEventType.UPDATE);
    event.setResourceType(InventoryResourceType.INSTANCE);
    event.setNewValue(Map.of("title", "value", "dates", Map.of("date1", "1234", "date2", "0000"), "subjects",
      List.of(Map.of("value", "subject1"), Map.of("value", "subject4"),
        Map.of("value", "subject2", "authorityId", "62ccd713-3433-4304-b5cd-636e859a911c"))));
    event.setOldValue(Map.of("title", "value2", "dates", Map.of("date1", "1233"), "subjects",
      List.of(Map.of("value", "subject3"), Map.of("value", "subject2"), Map.of("value", "subject4"))));

    try (MockedStatic<InventoryUtils> utilities = mockStatic(InventoryUtils.class)) {
      utilities.when(() -> InventoryUtils.extractUserId(event)).thenReturn(UUID.randomUUID().toString());

      var result = mapper.apply(event);

      assertEquals(UUID.fromString(event.getEventId()), result.eventId());
      assertEquals(new Timestamp(event.getEventTs()), result.eventDate());
      assertEquals(UUID.fromString(event.getEntityId()), result.entityId());
      assertEquals(InventoryEventType.UPDATE.name(), result.action());
      assertEquals(UUID.fromString(InventoryUtils.extractUserId(event)), result.userId());
      assertNotNull(result.diff());
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