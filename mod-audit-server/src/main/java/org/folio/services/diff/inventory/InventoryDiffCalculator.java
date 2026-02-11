package org.folio.services.diff.inventory;

import java.util.Map;
import org.folio.domain.diff.ChangeRecordDto;
import org.folio.util.inventory.InventoryResourceType;

public interface InventoryDiffCalculator {

  InventoryResourceType getResourceType();

  ChangeRecordDto calculateDiff(Map<String, Object> oldValue, Map<String, Object> newValue);
}
