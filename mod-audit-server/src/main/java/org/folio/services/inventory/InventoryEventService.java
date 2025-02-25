package org.folio.services.inventory;

import io.vertx.core.Future;
import java.sql.Timestamp;
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

  /**
   * Delete all MARC records which are expired
   *
   * @param tenantId id of tenant
   * @param expireOlderThan timestamp to expire records older than
   * @return Future void
   */
  Future<Void> expireRecords(String tenantId, Timestamp expireOlderThan);
}
