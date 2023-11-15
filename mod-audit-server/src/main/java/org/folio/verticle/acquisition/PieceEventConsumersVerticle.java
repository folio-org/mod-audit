package org.folio.verticle.acquisition;

import java.util.List;

import org.folio.kafka.AsyncRecordHandler;
import org.folio.util.AcquisitionEventType;
import org.folio.verticle.AbstractConsumersVerticle;
import org.springframework.stereotype.Component;

@Component
public class PieceEventConsumersVerticle extends AbstractConsumersVerticle {

  private final AsyncRecordHandler<String, String> pieceEventsHandler;

  public PieceEventConsumersVerticle(AsyncRecordHandler<String, String> pieceEventsHandler) {
    this.pieceEventsHandler = pieceEventsHandler;
  }

  @Override
  public List<String> getEvents() {
    return List.of(AcquisitionEventType.ACQ_PIECE_CHANGED.getTopicName());
  }

  @Override
  public AsyncRecordHandler<String, String> getHandler() {
    return pieceEventsHandler;
  }
}
