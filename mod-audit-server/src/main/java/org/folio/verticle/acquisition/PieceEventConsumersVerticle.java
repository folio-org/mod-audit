package org.folio.verticle.acquisition;

import java.util.List;

import org.folio.kafka.AsyncRecordHandler;
import org.folio.kafka.KafkaConfig;
import org.folio.util.AcquisitionEventType;
import org.folio.verticle.AbstractConsumersVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PieceEventConsumersVerticle extends AbstractConsumersVerticle {

  private final KafkaConfig kafkaConfig;
  private final AsyncRecordHandler<String, String> orderLineEventsHandler;

  @Autowired
  public PieceEventConsumersVerticle(KafkaConfig kafkaConfig, AsyncRecordHandler<String, String> orderLineEventsHandler) {
    this.kafkaConfig = kafkaConfig;
    this.orderLineEventsHandler = orderLineEventsHandler;
  }

  @Override
  public List<String> getEvents() {
    return List.of(AcquisitionEventType.ACQ_PIECE_CHANGED.getTopicName());
  }

  @Override
  public AsyncRecordHandler<String, String> getHandler() {
    return orderLineEventsHandler;
  }
}