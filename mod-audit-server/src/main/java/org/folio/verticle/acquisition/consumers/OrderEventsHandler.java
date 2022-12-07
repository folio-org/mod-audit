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
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.acquisition.OrderAuditEventsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderEventsHandler implements AsyncRecordHandler<String, String> {

  private static final Logger LOGGER = LogManager.getLogger();

  private OrderAuditEventsService orderAuditEventsService;

  private Vertx vertx;

  public OrderEventsHandler(@Autowired Vertx vertx,
                            @Autowired OrderAuditEventsService orderAuditEventsService) {
    this.vertx = vertx;
    this.orderAuditEventsService = orderAuditEventsService;
  }

  @Override
  public Future<String> handle(KafkaConsumerRecord<String, String> record) {

    Promise<String> result = Promise.promise();
    List<KafkaHeader> kafkaHeaders = record.headers();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(KafkaHeaderUtils.kafkaHeadersToMap(kafkaHeaders), vertx);
    OrderAuditEvent orderAuditEvent = new JsonObject(record.value()).mapTo(OrderAuditEvent.class);
    LOGGER.debug("Event was received with event type: {}", orderAuditEvent.getAction());

    orderAuditEventsService.collectData(orderAuditEvent, okapiConnectionParams.getTenantId());
    return result.future();
  }
}
