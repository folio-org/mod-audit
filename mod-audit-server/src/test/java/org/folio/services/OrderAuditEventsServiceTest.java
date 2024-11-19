package org.folio.services;

import static org.folio.utils.EntityUtils.ACTION_DATE_SORT_BY;
import static org.folio.utils.EntityUtils.DESC_ORDER;
import static org.folio.utils.EntityUtils.LIMIT;
import static org.folio.utils.EntityUtils.OFFSET;
import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createOrderAuditEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.folio.dao.acquisition.OrderEventsDao;
import org.folio.dao.acquisition.impl.OrderEventsDaoImpl;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.rest.jaxrs.model.OrderAuditEventCollection;
import org.folio.rest.persist.PostgresClient;
import org.folio.services.acquisition.impl.OrderAuditEventsServiceImpl;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class OrderAuditEventsServiceTest {

  @Mock
  private RowSet<Row> rowSet;
  @Mock
  private PostgresClient postgresClient;

  private OrderEventsDao orderEventsDao;
  private OrderAuditEventsServiceImpl orderAuditEventService;

  @BeforeEach
  public void setUp() throws Exception {
    try (var ignored = MockitoAnnotations.openMocks(this)) {
      var postgresClientFactory =  spy(new PostgresClientFactory(Vertx.vertx()));
      orderEventsDao = spy(new OrderEventsDaoImpl(postgresClientFactory));
      orderAuditEventService = new OrderAuditEventsServiceImpl(orderEventsDao);

      doReturn(postgresClient).when(postgresClientFactory).createInstance(TENANT_ID);
    }
  }

  @Test
  void shouldCallDaoForSuccessfulCase() {
    var orderAuditEvent = createOrderAuditEvent(UUID.randomUUID().toString());
    doReturn(Future.succeededFuture(rowSet)).when(postgresClient).execute(anyString(), any(Tuple.class));

    var saveFuture = orderAuditEventService.saveOrderAuditEvent(orderAuditEvent, TENANT_ID);
    saveFuture.onComplete(asyncResult -> assertTrue(asyncResult.succeeded()));

    verify(orderEventsDao, times(1)).save(orderAuditEvent, TENANT_ID);
  }

  @Test
  void shouldGetDto() {
    var id = UUID.randomUUID().toString();
    var orderAuditEvent = createOrderAuditEvent(id);
    var orderAuditEventCollection = new OrderAuditEventCollection().withOrderAuditEvents(List.of(orderAuditEvent)).withTotalItems(1);

    doReturn(Future.succeededFuture(orderAuditEventCollection)).when(orderEventsDao).getAuditEventsByOrderId(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString());

    var dto = orderAuditEventService.getAuditEventsByOrderId(id, ACTION_DATE_SORT_BY, DESC_ORDER, LIMIT, OFFSET, TENANT_ID);
    dto.onComplete(asyncResult -> {
      var orderAuditEventOptional = asyncResult.result();
      var orderAuditEventList = orderAuditEventOptional.getOrderAuditEvents();

      assertEquals(orderAuditEventList.get(0).getId(), id);
      assertEquals(OrderAuditEvent.Action.CREATE, orderAuditEventList.get(0).getAction());
    });

    verify(orderEventsDao, times(1)).getAuditEventsByOrderId(id, ACTION_DATE_SORT_BY, DESC_ORDER, LIMIT, OFFSET, TENANT_ID);
  }
}
