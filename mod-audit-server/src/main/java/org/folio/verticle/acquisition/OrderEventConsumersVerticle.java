package org.folio.verticle.acquisition;

import org.folio.kafka.AsyncRecordHandler;
import org.folio.kafka.KafkaConfig;
import org.folio.util.AcquisitionEventType;
import org.folio.verticle.AbstractConsumersVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderEventConsumersVerticle extends AbstractConsumersVerticle {
  @Autowired
  private KafkaConfig kafkaConfig;
  @Autowired
  private AsyncRecordHandler<String, String> orderEventsHandler;

  public List<String> getEvents() {
    return List.of(AcquisitionEventType.ACQ_ORDER_CHANGED.getTopicName());
  }

  public AsyncRecordHandler<String, String> getHandler() {
    return orderEventsHandler;
  }
}
