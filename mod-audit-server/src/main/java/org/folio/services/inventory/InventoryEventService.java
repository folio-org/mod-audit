package org.folio.services.inventory;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.rest.jaxrs.model.InventoryAuditCollection;
import org.folio.util.inventory.InventoryEvent;
import org.folio.util.inventory.InventoryResourceType;

public interface InventoryEventService {

  /**
   * Saves InventoryEvent
   *
   * @param inventoryEvent InventoryEvent
   * @param tenantId        id of tenant
   * @return
   */
  Future<RowSet<Row>> saveEvent(InventoryEvent inventoryEvent, String tenantId);

  /**
   * Retrieves InventoryEvent List
   *
   * @param resourceType resource type
   * @param entityId     entity id
   * @param eventDate    event date to seek from
   * @param tenantId     id of tenant
   * @return
   */
  Future<InventoryAuditCollection> getEvents(InventoryResourceType resourceType, String entityId, String eventDate,
                                             String tenantId);
}
