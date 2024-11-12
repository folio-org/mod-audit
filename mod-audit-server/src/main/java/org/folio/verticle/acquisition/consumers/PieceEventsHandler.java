package org.folio.verticle.acquisition.consumers;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.kafka.AsyncRecordHandler;
import org.folio.kafka.KafkaHeaderUtils;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.rest.jaxrs.model.PieceAuditEvent;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.acquisition.PieceAuditEventsService;
import org.springframework.stereotype.Component;

@Component
public class PieceEventsHandler implements AsyncRecordHandler<String, String> {

  private static final Logger LOGGER = LogManager.getLogger();

  private final PieceAuditEventsService pieceAuditEventsService;
  private final Vertx vertx;

  public PieceEventsHandler(Vertx vertx,
                            PieceAuditEventsService pieceAuditEventsService) {
    this.vertx = vertx;
    this.pieceAuditEventsService = pieceAuditEventsService;
  }

  @Override
  public Future<String> handle(KafkaConsumerRecord<String, String> kafkaConsumerRecord) {
    var kafkaHeaders = kafkaConsumerRecord.headers();
    var okapiConnectionParams = new OkapiConnectionParams(KafkaHeaderUtils.kafkaHeadersToMap(kafkaHeaders), vertx);
    var event = new JsonObject(kafkaConsumerRecord.value()).mapTo(PieceAuditEvent.class);
    LOGGER.info("handle:: Starting processing of Piece audit event with id: {} for piece id: {}", event.getId(), event.getPieceId());

    return pieceAuditEventsService.savePieceAuditEvent(event, okapiConnectionParams.getTenantId())
      .onSuccess(ar -> LOGGER.info("handle:: Piece audit event with id: {} has been processed for piece id: {}", event.getId(), event.getPieceId()))
      .onFailure(e -> {
        if (e instanceof DuplicateEventException) {
          LOGGER.info("handle:: Duplicate Piece audit event with id: {} for piece id: {} received, skipped processing", event.getId(), event.getPieceId());
        } else {
          LOGGER.error("Processing of Piece audit event with id: {} for piece id: {} has been failed", event.getId(), event.getPieceId(), e);
        }
      })
      .map(event.getId());
  }
}
