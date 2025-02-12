package org.folio.services.diff;

import org.folio.rest.external.Item;
import org.folio.util.inventory.InventoryResourceType;
import org.springframework.stereotype.Component;

@Component
public class ItemDiffCalculator extends DiffCalculator<Item> {

  @Override
  public InventoryResourceType getResourceType() {
    return InventoryResourceType.ITEM;
  }

  @Override
  protected Class<Item> getType() {
    return Item.class;
  }
}
