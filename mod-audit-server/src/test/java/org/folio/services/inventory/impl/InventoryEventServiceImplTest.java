package org.folio.services.inventory.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.util.List;
import java.util.UUID;
import org.folio.CopilotGenerated;
import org.folio.dao.inventory.InventoryEventDao;
import org.folio.services.inventory.InventoryEventService;
import org.folio.util.inventory.InventoryEvent;
import org.folio.util.inventory.InventoryEventType;
import org.folio.util.inventory.InventoryResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@CopilotGenerated
public class InventoryEventServiceImplTest {

  @Mock
  private RowSet<Row> rowSet;
  @Mock
  private InventoryEventDao inventoryEventDao;

  private InventoryEventService inventoryEventService;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    doReturn(InventoryResourceType.INSTANCE).when(inventoryEventDao).resourceType();
    inventoryEventService = new InventoryEventServiceImpl(List.of(inventoryEventDao));
  }

  @Test
  void shouldSaveEventSuccessfully() {
    var inventoryEvent = createInventoryEvent();
    doReturn(Future.succeededFuture(rowSet)).when(inventoryEventDao).save(any(), anyString());

    var saveFuture = inventoryEventService.saveEvent(inventoryEvent, "testTenant");
    saveFuture.onComplete(asyncResult -> assertTrue(asyncResult.succeeded()));

    verify(inventoryEventDao, times(1)).save(any(), anyString());
  }

  @Test
  void shouldFailToSaveEventWhenDaoNotFound() {
    var inventoryEvent = createInventoryEvent();
    inventoryEvent.setResourceType(InventoryResourceType.UNKNOWN);

    var saveFuture = inventoryEventService.saveEvent(inventoryEvent, "testTenant");
    saveFuture.onComplete(asyncResult -> assertTrue(asyncResult.failed()));

    verify(inventoryEventDao, times(0)).save(any(), anyString());
  }

  private InventoryEvent createInventoryEvent() {
    var inventoryEvent = new InventoryEvent();
    inventoryEvent.setEventId(UUID.randomUUID().toString());
    inventoryEvent.setEntityId(UUID.randomUUID().toString());
    inventoryEvent.setResourceType(InventoryResourceType.INSTANCE);
    inventoryEvent.setEventTs(System.currentTimeMillis());
    inventoryEvent.setType(InventoryEventType.CREATE);
    return inventoryEvent;
  }
}