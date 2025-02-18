package org.folio.services.diff;

import java.util.function.Supplier;
import org.folio.rest.external.Dates;
import org.folio.rest.external.Instance;
import org.folio.rest.external.Tags__1;
import org.folio.util.inventory.InventoryResourceType;
import org.springframework.stereotype.Component;

@Component
public class InstanceDiffCalculator extends DiffCalculator<Instance> {

  @Override
  public InventoryResourceType getResourceType() {
    return InventoryResourceType.INSTANCE;
  }

  @Override
  protected Supplier<Instance> access(Instance value) {
    return () -> {
      if (value.getDates() == null) {
        value.setDates(new Dates());
      }
      if (value.getTags() == null) {
        value.setTags(new Tags__1());
      }
      return value;
    };
  }

  @Override
  protected Class<Instance> getType() {
    return Instance.class;
  }
}
