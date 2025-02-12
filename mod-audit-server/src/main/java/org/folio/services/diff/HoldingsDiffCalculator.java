package org.folio.services.diff;

import org.folio.rest.external.HoldingsRecord;
import org.folio.util.inventory.InventoryResourceType;
import org.springframework.stereotype.Component;

@Component
public class HoldingsDiffCalculator extends DiffCalculator<HoldingsRecord> {

  @Override
  public InventoryResourceType getResourceType() {
    return InventoryResourceType.HOLDINGS;
  }

  @Override
  protected Class<HoldingsRecord> getType() {
    return HoldingsRecord.class;
  }
}
