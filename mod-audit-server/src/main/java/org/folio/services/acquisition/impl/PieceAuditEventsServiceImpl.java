package org.folio.services.acquisition.impl;

import static org.folio.util.ErrorUtils.handleFailures;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.acquisition.PieceEventsDao;
import org.folio.rest.jaxrs.model.PieceAuditEvent;
import org.folio.rest.jaxrs.model.PieceAuditEventCollection;
import org.folio.services.acquisition.PieceAuditEventsService;
import org.springframework.stereotype.Service;

@Service
public class PieceAuditEventsServiceImpl implements PieceAuditEventsService {
  private static final Logger LOGGER = LogManager.getLogger();

  private final PieceEventsDao pieceEventsDao;

  public PieceAuditEventsServiceImpl(PieceEventsDao pieceEventsDao) {
    this.pieceEventsDao = pieceEventsDao;
  }

  @Override
  public Future<RowSet<Row>> savePieceAuditEvent(PieceAuditEvent pieceAuditEvent, String tenantId) {
    LOGGER.debug("savePieceAuditEvent:: Trying to save piece audit event with id={} for tenantId={}", pieceAuditEvent.getPieceId(), tenantId);
    return pieceEventsDao.save(pieceAuditEvent, tenantId)
      .recover(throwable -> {
        LOGGER.error("handleFailures:: Could not save order audit event for Piece id: {} in tenantId: {}", pieceAuditEvent.getPieceId(), tenantId);
        return handleFailures(throwable, pieceAuditEvent.getId());
      });
  }

  @Override
  public Future<PieceAuditEventCollection> getAuditEventsByPieceId(String pieceId, String sortBy, String sortOrder, int limit, int offset, String tenantId) {
    LOGGER.debug("getAuditEventsByOrderLineId:: Trying to retrieve audit events for piece Id : {} and tenant Id : {}", pieceId, tenantId);
    return pieceEventsDao.getAuditEventsByPieceId(pieceId, sortBy, sortOrder, limit, offset, tenantId);
  }

  @Override
  public Future<PieceAuditEventCollection> getAuditEventsWithStatusChangesByPieceId(String pieceId, String sortBy, String sortOrder, int limit, int offset, String tenantId) {
    LOGGER.debug("getAuditEventsByOrderId:: Retrieving audit events with unique status changes for pieceId={} and tenantId={}", pieceId, tenantId);
    return pieceEventsDao.getAuditEventsWithStatusChangesByPieceId(pieceId, sortBy, sortOrder, limit, offset, tenantId);
  }
}
