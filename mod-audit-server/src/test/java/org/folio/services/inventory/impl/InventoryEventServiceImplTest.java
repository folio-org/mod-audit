package org.folio.services.inventory.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.util.List;
import java.util.UUID;
import org.folio.CopilotGenerated;
import org.folio.dao.inventory.impl.HoldingsEventDao;
import org.folio.dao.inventory.impl.InstanceEventDao;
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
  private InstanceEventDao instanceEventDao;
  @Mock
  private HoldingsEventDao holdingsEventDao;

  private InventoryEventService inventoryEventService;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    doCallRealMethod().when(instanceEventDao).resourceType();
    doCallRealMethod().when(holdingsEventDao).resourceType();
    inventoryEventService = new InventoryEventServiceImpl(List.of(instanceEventDao, holdingsEventDao));
  }

  @Test
  void shouldSaveInstanceSuccessfully() {
    var inventoryEvent = createInventoryEvent(InventoryResourceType.INSTANCE);
    doReturn(Future.succeededFuture(rowSet)).when(instanceEventDao).save(any(), anyString());

    var saveFuture = inventoryEventService.saveEvent(inventoryEvent, "testTenant");
    saveFuture.onComplete(asyncResult -> assertTrue(asyncResult.succeeded()));

    verify(instanceEventDao, times(1)).save(any(), anyString());
  }

  @Test
  void shouldSaveHoldingsSuccessfully() {
    var inventoryEvent = createInventoryEvent(InventoryResourceType.HOLDINGS);
    doReturn(Future.succeededFuture(rowSet)).when(holdingsEventDao).save(any(), anyString());

    var saveFuture = inventoryEventService.saveEvent(inventoryEvent, "testTenant");
    saveFuture.onComplete(asyncResult -> assertTrue(asyncResult.succeeded()));

    verify(holdingsEventDao, times(1)).save(any(), anyString());
  }

  @Test
  void shouldFailToSaveEventWhenDaoNotFound() {
    var inventoryEvent = createInventoryEvent(InventoryResourceType.UNKNOWN);

    var saveFuture = inventoryEventService.saveEvent(inventoryEvent, "testTenant");
    saveFuture.onComplete(asyncResult -> assertTrue(asyncResult.failed()));

    verify(instanceEventDao, times(0)).save(any(), anyString());
  }

  private InventoryEvent createInventoryEvent(InventoryResourceType resourceType) {
    var inventoryEvent = new InventoryEvent();
    inventoryEvent.setEventId(UUID.randomUUID().toString());
    inventoryEvent.setEntityId(UUID.randomUUID().toString());
    inventoryEvent.setResourceType(resourceType);
    inventoryEvent.setEventTs(System.currentTimeMillis());
    inventoryEvent.setType(InventoryEventType.CREATE);
    return inventoryEvent;
  }
}