package org.folio.verticle.acquisition;

import org.folio.kafka.AsyncRecordHandler;
import org.folio.kafka.KafkaConfig;
import org.folio.kafka.ProcessRecordErrorHandler;
import org.folio.util.AcquisitionEventType;
import org.folio.verticle.AbstractConsumersVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderEventConsumersVerticle extends AbstractConsumersVerticle {

  @Autowired
  @Qualifier("newKafkaConfig")
  private KafkaConfig kafkaConfig;

  @Autowired
  @Qualifier("orderEventHandler")
  private AsyncRecordHandler<String, String> orderEventHandler;

  @Autowired
  @Qualifier("orderEventErrorHandler")
  private ProcessRecordErrorHandler<String, String> orderEventErrorHandler;

  public List<String> getEvents() {
    return List.of(AcquisitionEventType.ACQ_ORDER_CHANGED.getTopicName());
  }

  public AsyncRecordHandler<String, String> getHandler() {
    return orderEventHandler;
  }

  public ProcessRecordErrorHandler<String, String> getErrorHandler() {
    return orderEventErrorHandler;
  }
}
