package org.folio.dao.acquisition;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.rest.jaxrs.model.OrderAuditEventDto;

import java.util.Optional;

public interface OrderEventsDao {

  /**
   * Saves orderAuditEvent entity to DB
   *
   * @param orderAuditEvent OrderAuditEvent entity to save
   * @param tenantId tenant id
   * @return future with created row
   */
  Future<RowSet<Row>> save(OrderAuditEvent orderAuditEvent, String tenantId);

  /**
   * Searches for OrderAuditEvent by id
   *
   * @param id OrderAuditEvent id
   * @return future with optional OrderAuditEvent
   */
  Future<Optional<OrderAuditEventDto>> getAcquisitionOrderAuditEventById(String id, String tenantId);
}
