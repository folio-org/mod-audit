package org.folio.verticle.acquisition.consumers.errorhandlers;

import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import org.folio.kafka.ProcessRecordErrorHandler;
import org.springframework.stereotype.Component;

@Component
public class OrderLineEventsErrorHandler implements ProcessRecordErrorHandler<String, String> {

  @Override
  public void handle(Throwable throwable, KafkaConsumerRecord<String, String> kafkaConsumerRecord) {
  }
}
