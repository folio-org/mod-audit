package org.folio.dao.inventory;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
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
   * Retrieves inventoryAuditEntity entity list from DB with filter by entityId
   * and seek by eventDate descending not including.
   *
   * @param entityId  entity id
   * @param eventDate event date to seek from
   * @param limit     number of record to be returned
   * @param tenantId  tenant id
   * @return future with result list
   */
  Future<List<InventoryAuditEntity>> get(UUID entityId, Timestamp eventDate, int limit, String tenantId);

  /**
   * Returns inventory resource type for the dao
   * @return InventoryResourceType
   */
  InventoryResourceType resourceType();
}
