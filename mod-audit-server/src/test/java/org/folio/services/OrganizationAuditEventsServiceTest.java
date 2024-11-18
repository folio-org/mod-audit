package org.folio.services;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.folio.CopilotGenerated;
import org.folio.dao.acquisition.OrganizationEventsDao;
import org.folio.dao.acquisition.impl.OrganizationEventsDaoImpl;
import org.folio.rest.jaxrs.model.OrganizationAuditEvent;
import org.folio.rest.jaxrs.model.OrganizationAuditEventCollection;
import org.folio.rest.persist.PostgresClient;
import org.folio.services.acquisition.impl.OrganizationAuditEventsServiceImpl;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;

import static org.folio.utils.EntityUtils.ACTION_DATE_SORT_BY;
import static org.folio.utils.EntityUtils.DESC_ORDER;
import static org.folio.utils.EntityUtils.LIMIT;
import static org.folio.utils.EntityUtils.OFFSET;
import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createOrganizationAuditEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@CopilotGenerated
public class OrganizationAuditEventsServiceTest {

  @Mock
  private RowSet<Row> rowSet;
  @Mock
  private PostgresClient postgresClient;

  private OrganizationEventsDao organizationEventsDao;
  private OrganizationAuditEventsServiceImpl organizationAuditEventService;

  @BeforeEach
  public void setUp() throws Exception {
    try (var ignored = MockitoAnnotations.openMocks(this)) {
      var postgresClientFactory =  spy(new PostgresClientFactory(Vertx.vertx()));
      organizationEventsDao = spy(new OrganizationEventsDaoImpl(postgresClientFactory));
      organizationAuditEventService = new OrganizationAuditEventsServiceImpl(organizationEventsDao);

      doReturn(postgresClient).when(postgresClientFactory).createInstance(TENANT_ID);
    }
  }

  @Test
  void shouldCallDaoForSuccessfulCase() {
    var organizationAuditEvent = createOrganizationAuditEvent(UUID.randomUUID().toString());
    doReturn(Future.succeededFuture(rowSet)).when(postgresClient).execute(anyString(), any(Tuple.class));

    var saveFuture = organizationAuditEventService.saveOrganizationAuditEvent(organizationAuditEvent, TENANT_ID);
    saveFuture.onComplete(asyncResult -> assertTrue(asyncResult.succeeded()));

    verify(organizationEventsDao, times(1)).save(organizationAuditEvent, TENANT_ID);
  }

  @Test
  void shouldGetDto() {
    var id = UUID.randomUUID().toString();
    var organizationAuditEvent = createOrganizationAuditEvent(id);
    var organizationAuditEventCollection = new OrganizationAuditEventCollection().withOrganizationAuditEvents(List.of(organizationAuditEvent)).withTotalItems(1);

    doReturn(Future.succeededFuture(organizationAuditEventCollection)).when(organizationEventsDao).getAuditEventsByOrganizationId(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString());

    var dto = organizationAuditEventService.getAuditEventsByOrganizationId(id, ACTION_DATE_SORT_BY, DESC_ORDER, LIMIT, OFFSET, TENANT_ID);
    dto.onComplete(asyncResult -> {
      var organizationAuditEventOptional = asyncResult.result();
      var organizationAuditEventList = organizationAuditEventOptional.getOrganizationAuditEvents();

      assertEquals(organizationAuditEventList.get(0).getId(), id);
      assertEquals(OrganizationAuditEvent.Action.CREATE, organizationAuditEventList.get(0).getAction());
    });

    verify(organizationEventsDao, times(1)).getAuditEventsByOrganizationId(id, ACTION_DATE_SORT_BY, DESC_ORDER, LIMIT, OFFSET, TENANT_ID);
  }
}
