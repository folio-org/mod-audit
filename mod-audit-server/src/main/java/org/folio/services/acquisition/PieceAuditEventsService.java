package org.folio.services.acquisition;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.rest.jaxrs.model.PieceAuditEvent;
import org.folio.rest.jaxrs.model.PieceAuditEventCollection;

public interface PieceAuditEventsService {

  /**
   * Saves Piece Audit Event
   *
   * @param pieceAuditEvent pieceAuditEvent
   * @param tenantId        id of tenant
   * @return
   */
  Future<RowSet<Row>> savePieceAuditEvent(PieceAuditEvent pieceAuditEvent, String tenantId);

  /**
   * Searches for piece audit events by piece id
   *
   * @param pieceId   piece id
   * @param sortBy    sort by
   * @param sortOrder sort order
   * @param limit     limit
   * @param offset    offset
   * @return future with PieceAuditEventCollection
   */
  Future<PieceAuditEventCollection> getAuditEventsByPieceId(String pieceId, String sortBy, String sortOrder,
                                                            int limit, int offset, String tenantId);

  /**
   * Searches for piece audit events which has unique status changes by piece id
   * @param pieceId   piece id
   * @param sortBy    sort by
   * @param sortOrder sort order
   * @param limit     limit
   * @param offset    offset
   * @return future with PieceAuditEventCollection
   */
  Future<PieceAuditEventCollection> getAuditEventsWithStatusChangesByPieceId(String pieceId, String sortBy, String sortOrder,
                                                                        int limit, int offset, String tenantId);
}
