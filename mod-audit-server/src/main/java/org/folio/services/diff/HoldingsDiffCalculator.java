package org.folio.services.diff;

import java.util.function.Supplier;
import org.folio.rest.external.HoldingsRecord;
import org.folio.rest.external.ReceivingHistory;
import org.folio.rest.external.Tags;
import org.folio.util.inventory.InventoryResourceType;
import org.springframework.stereotype.Component;

@Component
public class HoldingsDiffCalculator extends DiffCalculator<HoldingsRecord> {

  @Override
  public InventoryResourceType getResourceType() {
    return InventoryResourceType.HOLDINGS;
  }

  @Override
  protected Supplier<HoldingsRecord> access(HoldingsRecord value) {
    return () -> {
      if (value.getTags() == null) {
        value.setTags(new Tags());
      }
      if (value.getReceivingHistory() == null) {
        value.setReceivingHistory(new ReceivingHistory());
      }
      return value;
    };
  }

  @Override
  protected Class<HoldingsRecord> getType() {
    return HoldingsRecord.class;
  }
}
