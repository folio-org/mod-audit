package org.folio.mapper;

import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.folio.dao.inventory.InventoryAuditEntity;
import org.folio.rest.jaxrs.model.InventoryAuditItem;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryEntityToAuditItemMapper implements Function<InventoryAuditEntity, InventoryAuditItem> {

  private final DiffMapper diffMapper;

  @Override
  public InventoryAuditItem apply(InventoryAuditEntity auditEntity) {
    return new InventoryAuditItem()
      .withEventId(auditEntity.eventId().toString())
      .withEventTs(auditEntity.eventDate().getTime())
      .withEventDate(auditEntity.eventDate())
      .withEntityId(auditEntity.entityId().toString())
      .withAction(auditEntity.action())
      .withUserId(auditEntity.userId().toString())
      .withDiff(diffMapper.map(auditEntity.diff()));
  }
}
