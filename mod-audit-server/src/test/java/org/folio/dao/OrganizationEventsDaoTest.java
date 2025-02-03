package org.folio.dao;

import io.vertx.core.Vertx;
import io.vertx.pgclient.PgException;
import org.folio.CopilotGenerated;
import org.folio.dao.acquisition.impl.OrganizationEventsDaoImpl;
import org.folio.rest.jaxrs.model.OrganizationAuditEvent;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.UUID;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createOrganizationAuditEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@CopilotGenerated
public class OrganizationEventsDaoTest {

  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());
  @InjectMocks
  OrganizationEventsDaoImpl organizationEventDao;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    organizationEventDao = new OrganizationEventsDaoImpl(postgresClientFactory);
  }

  @Test
  void shouldCreateEventProcessed() {
    var organizationAuditEvent = createOrganizationAuditEvent(UUID.randomUUID().toString());

    var saveFuture = organizationEventDao.save(organizationAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> assertTrue(ar.succeeded()));
    verify(postgresClientFactory, times(1)).createInstance(TENANT_ID);
  }

  @Test
  void shouldThrowConstraintViolation() {
    var organizationAuditEvent = createOrganizationAuditEvent(UUID.randomUUID().toString());

    var saveFuture = organizationEventDao.save(organizationAuditEvent, TENANT_ID);
    saveFuture.onComplete(ar -> {
      var reSaveFuture = organizationEventDao.save(organizationAuditEvent, TENANT_ID);
      reSaveFuture.onComplete(re -> {
        assertTrue(re.failed());
        assertTrue(re.cause() instanceof PgException);
        assertEquals("ERROR: duplicate key value violates unique constraint \"acquisition_organization_log_pkey\" (23505)", re.cause().getMessage());
      });
    });
    verify(postgresClientFactory, times(1)).createInstance(TENANT_ID);
  }

  @Test
  void shouldGetCreatedEvent() {
    var id = UUID.randomUUID().toString();
    var organizationAuditEvent = createOrganizationAuditEvent(id);

    organizationEventDao.save(organizationAuditEvent, TENANT_ID);

    var dto = organizationEventDao.getAuditEventsByOrganizationId(id, "action_date", "desc", 1, 1, TENANT_ID);
    dto.onComplete(ar -> {
      var organizationAuditEventOptional = ar.result();
      var organizationAuditEventList = organizationAuditEventOptional.getOrganizationAuditEvents();

      assertEquals(organizationAuditEventList.get(0).getId(), id);
      assertEquals(OrganizationAuditEvent.Action.CREATE.value(), organizationAuditEventList.get(0).getAction().value());
    });
    verify(postgresClientFactory, times(2)).createInstance(TENANT_ID);
  }
}
