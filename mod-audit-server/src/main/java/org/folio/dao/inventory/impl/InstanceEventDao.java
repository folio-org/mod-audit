package org.folio.dao.inventory.impl;

import org.folio.util.inventory.InventoryResourceType;
import org.folio.util.PostgresClientFactory;
import org.springframework.stereotype.Repository;

@Repository
public class InstanceEventDao extends InventoryEventDaoImpl {

  protected InstanceEventDao(PostgresClientFactory pgClientFactory) {
    super(pgClientFactory);
  }

  @Override
  public InventoryResourceType resourceType() {
    return InventoryResourceType.INSTANCE;
  }
}
