package org.folio.mapper;

import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.folio.dao.inventory.InventoryAuditEntity;
import org.folio.rest.jaxrs.model.InventoryAuditCollection;
import org.folio.rest.jaxrs.model.InventoryAuditItem;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryEntitiesToAuditCollectionMapper implements Function<List<InventoryAuditEntity>, InventoryAuditCollection> {

  private final Function<InventoryAuditEntity, InventoryAuditItem> entityToItemMapper;

  @Override
  public InventoryAuditCollection apply(List<InventoryAuditEntity> inventoryAuditEntities) {
    var items = inventoryAuditEntities.stream()
      .map(entityToItemMapper)
      .toList();
    return new InventoryAuditCollection().withInventoryAuditItems(items);
  }
}
