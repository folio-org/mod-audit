package org.folio.verticle.acquisition.consumers;

import io.vertx.core.Future;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import org.folio.kafka.AsyncRecordHandler;
import org.springframework.stereotype.Component;

@Component
public class OrderEventsHandler implements AsyncRecordHandler<String, String> {

  @Override
  public Future<String> handle(KafkaConsumerRecord<String, String> record) {
    return Future.succeededFuture();
  }
}
