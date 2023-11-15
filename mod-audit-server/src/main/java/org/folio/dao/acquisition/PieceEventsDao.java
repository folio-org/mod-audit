package org.folio.dao.acquisition;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.rest.jaxrs.model.PieceAuditEvent;
import org.folio.rest.jaxrs.model.PieceAuditEventCollection;

public interface PieceEventsDao {

  /**
   * Saves pieceAuditEvent entity to DB
   *
   * @param pieceAuditEvent pieceAuditEvent entity to save
   * @param tenantId        tenant id
   * @return future with created row
   */
  Future<RowSet<Row>> save(PieceAuditEvent pieceAuditEvent, String tenantId);

  /**
   * Searches for piece audit events by id
   *
   * @param pieceId   piece id
   * @param sortBy    sort by
   * @param sortOrder sort order
   * @param limit     limit
   * @param offset    offset
   * @param tenantId  tenant id
   * @return future with PieceAuditEventCollection
   */
  Future<PieceAuditEventCollection> getAuditEventsByPieceId(String pieceId, String sortBy, String sortOrder,
                                                            int limit, int offset, String tenantId);

  /**
   * Searches for piece audit events with status changes by piece id
   * @param pieceId   piece id
   * @param sortBy    sort by
   * @param sortOrder sort order
   * @param limit     limit
   * @param offset    offset
   * @param tenantId  tenant id
   * @return future with PieceAuditEventCollection
   */
  Future<PieceAuditEventCollection> getAuditEventsWithStatusChangesByPieceId(String pieceId, String sortBy, String sortOrder,
                                                                            int limit, int offset, String tenantId);
}
