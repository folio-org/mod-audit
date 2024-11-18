package org.folio.dao.acquisition;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.rest.jaxrs.model.OrganizationAuditEvent;
import org.folio.rest.jaxrs.model.OrganizationAuditEventCollection;

public interface OrganizationEventsDao {

  /**
   * Saves organizationAuditEvent entity to DB
   *
   * @param organizationAuditEvent OrganizationAuditEvent entity to save
   * @param tenantId tenant id
   * @return future with created row
   */
  Future<RowSet<Row>> save(OrganizationAuditEvent organizationAuditEvent, String tenantId);

  /**
   * Searches for organization audit events by id
   *
   * @param organizationId   organization id
   * @param sortBy    sort by
   * @param sortOrder sort order
   * @param limit     limit
   * @param offset    offset
   * @param tenantId  tenant id
   * @return future with OrganizationAuditEventCollection
   */
  Future<OrganizationAuditEventCollection> getAuditEventsByOrganizationId(String organizationId, String sortBy, String sortOrder, int limit, int offset, String tenantId);
}
