package org.folio.mapper.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Timestamp;
import java.util.UUID;
import org.folio.dao.user.UserAuditEntity;
import org.folio.domain.diff.ChangeRecordDto;
import org.folio.mapper.DiffMapperImpl;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class UserEntityToAuditItemMapperTest {

  @Spy
  private DiffMapperImpl diffMapper;
  @InjectMocks
  private UserEntityToAuditItemMapper mapper;

  @Test
  void shouldMapUserAuditEntityToUserAuditItem() {
    var performedBy = UUID.randomUUID();
    var auditEntity = new UserAuditEntity(
      UUID.randomUUID(),
      new Timestamp(System.currentTimeMillis()),
      UUID.randomUUID(),
      "CREATE",
      performedBy,
      new ChangeRecordDto()
    );

    var result = mapper.apply(auditEntity);

    assertEquals(auditEntity.eventId().toString(), result.getEventId());
    assertEquals(auditEntity.eventDate(), result.getEventDate());
    assertEquals(auditEntity.eventDate().getTime(), result.getEventTs());
    assertEquals(auditEntity.userId().toString(), result.getUserId());
    assertEquals(auditEntity.action(), result.getAction());
    assertEquals(performedBy.toString(), result.getPerformedBy());
    assertNotNull(result.getDiff());
  }

  @Test
  void shouldMapUserAuditEntityWithNullPerformedBy() {
    var auditEntity = new UserAuditEntity(
      UUID.randomUUID(),
      new Timestamp(System.currentTimeMillis()),
      UUID.randomUUID(),
      "CREATE",
      null,
      new ChangeRecordDto()
    );

    var result = mapper.apply(auditEntity);

    assertEquals(auditEntity.eventId().toString(), result.getEventId());
    assertEquals(auditEntity.userId().toString(), result.getUserId());
    assertEquals(auditEntity.action(), result.getAction());
    assertNull(result.getPerformedBy());
    assertNotNull(result.getDiff());
  }

  @Test
  void shouldMapUserAuditEntityWithNullDiff() {
    var auditEntity = new UserAuditEntity(
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
    assertEquals(auditEntity.userId().toString(), result.getUserId());
    assertEquals(auditEntity.action(), result.getAction());
    assertEquals(auditEntity.performedBy().toString(), result.getPerformedBy());
    assertNull(result.getDiff());
  }
}
