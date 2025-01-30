package org.folio.dao.inventory;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.util.inventory.InventoryResourceType;

public interface InventoryEventDao {

  /**
   * Saves inventoryAuditEntity entity to DB
   *
   * @param inventoryAuditEntity InventoryAuditEntity entity to save
   * @param tenantId        tenant id
   * @return future with created row
   */
  Future<RowSet<Row>> save(InventoryAuditEntity inventoryAuditEntity, String tenantId);

  /**
   * Returns inventory resource type for the dao
   * @return InventoryResourceType
   */
  InventoryResourceType resourceType();
}
