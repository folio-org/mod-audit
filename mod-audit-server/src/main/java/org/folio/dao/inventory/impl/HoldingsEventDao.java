package org.folio.dao.inventory.impl;

import org.folio.util.PostgresClientFactory;
import org.folio.util.inventory.InventoryResourceType;
import org.springframework.stereotype.Repository;

@Repository
public class HoldingsEventDao extends InventoryEventDaoImpl {

  public HoldingsEventDao(PostgresClientFactory pgClientFactory) {
    super(pgClientFactory);
  }

  @Override
  public InventoryResourceType resourceType() {
    return InventoryResourceType.HOLDINGS;
  }
}
