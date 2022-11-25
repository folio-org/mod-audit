package org.folio.verticle.acquisition;

import org.folio.kafka.AsyncRecordHandler;
import org.folio.kafka.KafkaConfig;
import org.folio.kafka.ProcessRecordErrorHandler;
import org.folio.util.AcquisitionEventType;
import org.folio.verticle.AbstractConsumersVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderLineEventConsumersVerticle extends AbstractConsumersVerticle {
  @Autowired
  private KafkaConfig kafkaConfig;
  @Autowired
  private AsyncRecordHandler<String, String> orderLineEventsHandler;
  @Autowired
  private ProcessRecordErrorHandler<String, String> orderLineEventsErrorHandler;

  @Override
  public List<String> getEvents() {
    return List.of(AcquisitionEventType.ACQ_ORDER_LINE_CHANGED.getTopicName());
  }

  @Override
  public AsyncRecordHandler<String, String> getHandler() {
    return orderLineEventsHandler;
  }
}
