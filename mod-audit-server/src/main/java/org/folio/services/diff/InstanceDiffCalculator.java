package org.folio.services.diff;

import org.folio.rest.external.Instance;
import org.folio.util.inventory.InventoryResourceType;
import org.springframework.stereotype.Component;

@Component
public class InstanceDiffCalculator extends DiffCalculator<Instance> {

  @Override
  public InventoryResourceType getResourceType() {
    return InventoryResourceType.INSTANCE;
  }

  @Override
  protected Class<Instance> getType() {
    return Instance.class;
  }
}
