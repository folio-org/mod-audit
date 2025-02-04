package org.folio.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import org.folio.dao.inventory.InventoryAuditEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InventoryEntityToAuditItemMapperTest {

  private InventoryEntityToAuditItemMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new InventoryEntityToAuditItemMapper();
  }

  @Test
  void shouldMapInventoryAuditEntityToInventoryAuditItem() {
    var auditEntity = new InventoryAuditEntity(
      UUID.randomUUID(),
      new Timestamp(System.currentTimeMillis()),
      UUID.randomUUID(),
      "CREATE",
      UUID.randomUUID(),
      Map.of("key", "value")
    );

    var result = mapper.apply(auditEntity);

    assertEquals(auditEntity.eventId().toString(), result.getEventId());
    assertEquals(auditEntity.eventDate(), result.getEventDate());
    assertEquals(auditEntity.entityId().toString(), result.getEntityId());
    assertEquals(auditEntity.action(), result.getAction());
    assertEquals(auditEntity.userId().toString(), result.getUserId());
    assertEquals(auditEntity.diff(), result.getDiff());
  }

  @Test
  void shouldMapInventoryAuditEntityWithNullDiff() {
    var auditEntity = new InventoryAuditEntity(
      UUID.randomUUID(),
      new Timestamp(System.currentTimeMillis()),
      UUID.randomUUID(),
      "DELETE",
      UUID.randomUUID(),
      null
    );

    var result = mapper.apply(auditEntity);

    assertEquals(auditEntity.eventId().toString(), result.getEventId());
    assertEquals(auditEntity.eventDate(), result.getEventDate());
    assertEquals(auditEntity.entityId().toString(), result.getEntityId());
    assertEquals(auditEntity.action(), result.getAction());
    assertEquals(auditEntity.userId().toString(), result.getUserId());
    assertEquals(auditEntity.diff(), result.getDiff());
  }
}