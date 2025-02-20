package org.folio.services.inventory;

import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.InventoryAuditCollection;
import org.folio.util.inventory.InventoryEvent;
import org.folio.util.inventory.InventoryResourceType;

public interface InventoryEventService {

  /**
   * Saves InventoryEvent
   *
   * @param inventoryEvent InventoryEvent
   * @param tenantId       id of tenant
   * @return Future with event id
   */
  Future<String> processEvent(InventoryEvent inventoryEvent, String tenantId);

  /**
   * Retrieves InventoryEvent List
   *
   * @param resourceType resource type
   * @param entityId     entity id
   * @param eventTs      event timestamp to seek from
   * @param tenantId     id of tenant
   * @return Future with InventoryAuditCollection
   */
  Future<InventoryAuditCollection> getEvents(InventoryResourceType resourceType, String entityId, String eventTs,
                                             String tenantId);
}
