package org.folio.verticle.acquisition.consumers;

import java.util.List;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.producer.KafkaHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.kafka.AsyncRecordHandler;
import org.folio.kafka.KafkaHeaderUtils;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.rest.jaxrs.model.PieceAuditEvent;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.acquisition.PieceAuditEventsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PieceEventsHandler implements AsyncRecordHandler<String, String> {

  private static final Logger LOGGER = LogManager.getLogger();

  private PieceAuditEventsService pieceAuditEventsService;
  private Vertx vertx;

  public PieceEventsHandler(@Autowired Vertx vertx,
                            @Autowired PieceAuditEventsService pieceAuditEventsService) {
    this.pieceAuditEventsService = pieceAuditEventsService;
    this.vertx = vertx;
  }

  @Override
  public Future<String> handle(KafkaConsumerRecord<String, String> kafkaConsumerRecord) {
    Promise<String> result = Promise.promise();
    List<KafkaHeader> kafkaHeaders = kafkaConsumerRecord.headers();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(KafkaHeaderUtils.kafkaHeadersToMap(kafkaHeaders), vertx);
    PieceAuditEvent event = new JsonObject(kafkaConsumerRecord.value()).mapTo(PieceAuditEvent.class);
    LOGGER.info("handle:: Starting processing of Piece audit event with id: {} for piece id: {}", event.getId(), event.getPieceId());

    pieceAuditEventsService.savePieceAuditEvent(event, okapiConnectionParams.getTenantId())
      .onSuccess(ar -> {
        LOGGER.info("handle:: Piece audit event with id: {} has been processed for piece id: {}", event.getId(), event.getPieceId());
        result.complete(event.getId());
      })
      .onFailure(e -> {
        if (e instanceof DuplicateEventException) {
          LOGGER.info("handle:: Duplicate Piece audit event with id: {} for piece id: {} received, skipped processing", event.getId(), event.getPieceId());
          result.complete(event.getId());
        } else {
          LOGGER.error("Processing of Piece audit event with id: {} for piece id: {} has been failed", event.getId(), event.getPieceId(), e);
          result.fail(e);
        }
      });

    return result.future();
  }
}
