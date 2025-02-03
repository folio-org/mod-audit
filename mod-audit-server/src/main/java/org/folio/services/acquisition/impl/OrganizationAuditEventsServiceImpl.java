package org.folio.services.acquisition.impl;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.acquisition.OrganizationEventsDao;
import org.folio.rest.jaxrs.model.OrganizationAuditEvent;
import org.folio.rest.jaxrs.model.OrganizationAuditEventCollection;
import org.folio.services.acquisition.OrganizationAuditEventsService;
import org.springframework.stereotype.Service;

import static org.folio.util.ErrorUtils.handleFailures;

@Service
public class OrganizationAuditEventsServiceImpl implements OrganizationAuditEventsService {

  private static final Logger LOGGER = LogManager.getLogger();

  private final OrganizationEventsDao organizationEventsDao;

  public OrganizationAuditEventsServiceImpl(OrganizationEventsDao organizationEvenDao) {
    this.organizationEventsDao = organizationEvenDao;
  }

  @Override
  public Future<RowSet<Row>> saveOrganizationAuditEvent(OrganizationAuditEvent organizationAuditEvent, String tenantId) {
    LOGGER.debug("saveOrganizationAuditEvent:: Saving organization audit event with organizationId={} for tenantId={}", organizationAuditEvent.getOrganizationId(), tenantId);
    return organizationEventsDao.save(organizationAuditEvent, tenantId)
      .recover(throwable -> {
        LOGGER.error("handleFailures:: Could not save organization audit event for Organization id: {} in tenantId: {}", organizationAuditEvent.getOrganizationId(), tenantId);
        return handleFailures(throwable, organizationAuditEvent.getId());
      });
  }

  @Override
  public Future<OrganizationAuditEventCollection> getAuditEventsByOrganizationId(String organizationId, String sortBy, String sortOrder, int limit, int offset, String tenantId) {
    LOGGER.debug("getAuditEventsByOrganizationId:: Retrieving audit events for organizationId={} and tenantId={}", organizationId, tenantId);
    return organizationEventsDao.getAuditEventsByOrganizationId(organizationId, sortBy, sortOrder, limit, offset, tenantId);
  }
}
