package org.folio.services.inventory;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.util.inventory.InventoryEvent;

public interface InventoryEventService {

  /**
   * Saves InventoryEvent
   *
   * @param inventoryEvent InventoryEvent
   * @param tenantId        id of tenant
   * @return
   */
  Future<RowSet<Row>> saveEvent(InventoryEvent inventoryEvent, String tenantId);
}
