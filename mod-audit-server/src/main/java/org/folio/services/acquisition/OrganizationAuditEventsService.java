package org.folio.services.acquisition;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.rest.jaxrs.model.OrganizationAuditEvent;
import org.folio.rest.jaxrs.model.OrganizationAuditEventCollection;

public interface OrganizationAuditEventsService {

  /**
   * Saves OrganizationAuditEvent
   *
   * @param organizationAuditEvent
   * @param tenantId id of tenant
   * @return successful future if event has not been processed, or failed future otherwise
   */
  Future<RowSet<Row>> saveOrganizationAuditEvent(OrganizationAuditEvent organizationAuditEvent, String tenantId);

  /**
   * Searches for organization audit events by organization id
   *
   * @param organizationId   organization id
   * @param sortBy    sort by
   * @param sortOrder sort order
   * @param limit     limit
   * @param offset    offset
   * @return future with OrganizationAuditEventCollection
   */
  Future<OrganizationAuditEventCollection> getAuditEventsByOrganizationId(String organizationId, String sortBy, String sortOrder, int limit, int offset, String tenantId);
}
