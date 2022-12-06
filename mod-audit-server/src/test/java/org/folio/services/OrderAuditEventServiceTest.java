package org.folio.services;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.dao.OrderEventDao;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.services.impl.OrderAuditEventServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(VertxUnitRunner.class)
public class OrderAuditEventServiceTest {

  private static final String TENANT_ID = "diku";

  @Mock
  private OrderEventDao orderEventDao;

  @InjectMocks
  OrderAuditEventServiceImpl orderAuditEventService;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    orderAuditEventService = new OrderAuditEventServiceImpl(orderEventDao);
  }

  @Test
  public void shouldCallDaoForSuccessfulCase() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("name","jsonTest");
    OrderAuditEvent orderAuditEvent = new OrderAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(OrderAuditEvent.Action.CREATE)
      .withOrderId(UUID.randomUUID().toString())
      .withUserId(UUID.randomUUID().toString())
      .withEventDate(LocalDateTime.now())
      .withActionDate(LocalDateTime.now())
      .withOrderSnapshot(jsonObject);

    when(orderEventDao.save(orderAuditEvent, TENANT_ID)).thenReturn(Future.succeededFuture());

    orderAuditEventService.collectData(orderAuditEvent, TENANT_ID);

    verify(orderEventDao).save(orderAuditEvent, TENANT_ID);
  }

}
