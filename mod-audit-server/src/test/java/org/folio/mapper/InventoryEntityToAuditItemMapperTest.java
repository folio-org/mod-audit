package org.folio.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Timestamp;
import java.util.UUID;
import org.folio.dao.inventory.InventoryAuditEntity;
import org.folio.domain.diff.ChangeRecordDto;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class InventoryEntityToAuditItemMapperTest {

  @Spy
  private DiffMapperImpl diffMapper;
  @InjectMocks
  private InventoryEntityToAuditItemMapper mapper;

  @Test
  void shouldMapInventoryAuditEntityToInventoryAuditItem() {
    var auditEntity = new InventoryAuditEntity(
      UUID.randomUUID(),
      new Timestamp(System.currentTimeMillis()),
      UUID.randomUUID(),
      "CREATE",
      UUID.randomUUID(),
      new ChangeRecordDto()
    );

    var result = mapper.apply(auditEntity);

    assertEquals(auditEntity.eventId().toString(), result.getEventId());
    assertEquals(auditEntity.eventDate(), result.getEventDate());
    assertEquals(auditEntity.entityId().toString(), result.getEntityId());
    assertEquals(auditEntity.action(), result.getAction());
    assertEquals(auditEntity.userId().toString(), result.getUserId());
    assertNotNull(result.getDiff());
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
    assertNull(result.getDiff());
  }
}