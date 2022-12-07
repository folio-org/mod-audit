package org.folio.verticle.acquisition.consumers;

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
import org.folio.rest.jaxrs.model.OrderLineAuditEvent;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.acquisition.OrderLineAuditEventsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderLineEventsHandler implements AsyncRecordHandler<String, String> {

  private static final Logger LOGGER = LogManager.getLogger();

  private OrderLineAuditEventsService orderLineAuditEventsService;

  private Vertx vertx;

  public OrderLineEventsHandler(@Autowired Vertx vertx,
                            @Autowired OrderLineAuditEventsService orderLineAuditEventsService) {
    this.vertx = vertx;
    this.orderLineAuditEventsService = orderLineAuditEventsService;
  }

  @Override
  public Future<String> handle(KafkaConsumerRecord<String, String> record) {
    Promise<String> result = Promise.promise();
    List<KafkaHeader> kafkaHeaders = record.headers();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(KafkaHeaderUtils.kafkaHeadersToMap(kafkaHeaders), vertx);
    OrderLineAuditEvent orderLineAuditEvent = new JsonObject(record.value()).mapTo(OrderLineAuditEvent.class);
    LOGGER.debug("Event was received with event type: {}", orderLineAuditEvent.getAction());

    orderLineAuditEventsService.saveOrderLineAuditEvent(orderLineAuditEvent, okapiConnectionParams.getTenantId());
    return result.future();
  }
}
