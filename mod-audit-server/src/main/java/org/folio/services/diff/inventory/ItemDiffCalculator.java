package org.folio.services.diff.inventory;

import java.util.function.Supplier;
import org.folio.rest.external.EffectiveCallNumberComponents;
import org.folio.rest.external.Item;
import org.folio.rest.external.LastCheckIn;
import org.folio.rest.external.Status;
import org.folio.rest.external.Tags__2;
import org.folio.services.diff.DiffCalculator;
import org.folio.util.inventory.InventoryResourceType;
import org.springframework.stereotype.Component;

@Component
public class ItemDiffCalculator extends DiffCalculator<Item> implements InventoryDiffCalculator {

  @Override
  public InventoryResourceType getResourceType() {
    return InventoryResourceType.ITEM;
  }

  @Override
  protected Supplier<Item> access(Item value) {
    return () -> {
      if (value.getTags() == null) {
        value.setTags(new Tags__2());
      }
      if (value.getEffectiveCallNumberComponents() == null) {
        value.setEffectiveCallNumberComponents(new EffectiveCallNumberComponents());
      }
      if (value.getLastCheckIn() == null) {
        value.setLastCheckIn(new LastCheckIn());
      }
      if (value.getStatus() == null) {
        value.setStatus(new Status());
      }
      return value;
    };
  }

  @Override
  protected Class<Item> getType() {
    return Item.class;
  }
}
