package org.folio.services;

import static org.folio.utils.EntityUtils.ACTION_DATE_SORT_BY;
import static org.folio.utils.EntityUtils.DESC_ORDER;
import static org.folio.utils.EntityUtils.LIMIT;
import static org.folio.utils.EntityUtils.OFFSET;
import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createOrderLineAuditEvent;
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
import org.folio.dao.acquisition.OrderLineEventsDao;
import org.folio.dao.acquisition.impl.OrderLineEventsDaoImpl;
import org.folio.rest.jaxrs.model.OrderLineAuditEvent;
import org.folio.rest.jaxrs.model.OrderLineAuditEventCollection;
import org.folio.rest.persist.PostgresClient;
import org.folio.services.acquisition.impl.OrderLineAuditEventsServiceImpl;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class OrderLineAuditEventsServiceTest {

  @Mock
  private RowSet<Row> rowSet;
  @Mock
  private PostgresClient postgresClient;

  private OrderLineEventsDao orderLineEventsDao;
  private OrderLineAuditEventsServiceImpl orderLineAuditEventService;

  @BeforeEach
  public void setUp() throws Exception {
    try (var ignored = MockitoAnnotations.openMocks(this)) {
      var postgresClientFactory =  spy(new PostgresClientFactory(Vertx.vertx()));
      orderLineEventsDao = spy(new OrderLineEventsDaoImpl(postgresClientFactory));
      orderLineAuditEventService = new OrderLineAuditEventsServiceImpl(orderLineEventsDao);

      doReturn(postgresClient).when(postgresClientFactory).createInstance(TENANT_ID);
    }
  }

  @Test
  public void shouldCallDaoForSuccessfulCase() {
    var orderLineAuditEvent = createOrderLineAuditEvent(UUID.randomUUID().toString());
    doReturn(Future.succeededFuture(rowSet)).when(postgresClient).execute(anyString(), any(Tuple.class));

    var saveFuture = orderLineAuditEventService.saveOrderLineAuditEvent(orderLineAuditEvent, TENANT_ID);
    saveFuture.onComplete(asyncResult -> assertTrue(asyncResult.succeeded()));

    verify(orderLineEventsDao, times(1)).save(orderLineAuditEvent, TENANT_ID);
  }

  @Test
  void shouldGetOrderLineDto() {
    var id = UUID.randomUUID().toString();
    var orderLineAuditEvent = createOrderLineAuditEvent(id);
    var orderLineAuditEventCollection = new OrderLineAuditEventCollection().withOrderLineAuditEvents(List.of(orderLineAuditEvent)).withTotalItems(1);

    doReturn(Future.succeededFuture(orderLineAuditEventCollection)).when(orderLineEventsDao).getAuditEventsByOrderLineId(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString());

    var dto = orderLineAuditEventService.getAuditEventsByOrderLineId(id, ACTION_DATE_SORT_BY, DESC_ORDER, LIMIT, OFFSET, TENANT_ID);
    dto.onComplete(asyncResult -> {
      var orderLineAuditEventOptional = asyncResult.result();
      var orderLineAuditEventList = orderLineAuditEventOptional.getOrderLineAuditEvents();

      assertEquals(orderLineAuditEventList.get(0).getId(), id);
      assertEquals(OrderLineAuditEvent.Action.CREATE, orderLineAuditEventList.get(0).getAction());
    });

    verify(orderLineEventsDao, times(1)).getAuditEventsByOrderLineId(id, ACTION_DATE_SORT_BY, DESC_ORDER, LIMIT, OFFSET, TENANT_ID);
  }
}
