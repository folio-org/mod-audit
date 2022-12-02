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
import org.folio.rest.jaxrs.model.Event;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.OrderAuditEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderEventsHandler implements AsyncRecordHandler<String, String> {

  private static final Logger LOGGER = LogManager.getLogger();

  public static final String RECORD_ID_HEADER = "recordId";
  private OrderAuditEventService orderAuditEventService;

  private Vertx vertx;

  public OrderEventsHandler(@Autowired Vertx vertx,
                            @Autowired OrderAuditEventService orderAuditEventService) {
    this.vertx = vertx;
    this.orderAuditEventService = orderAuditEventService;
  }

  @Override
  public Future<String> handle(KafkaConsumerRecord<String, String> record) {

    Promise<String> result = Promise.promise();
    List<KafkaHeader> kafkaHeaders = record.headers();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(KafkaHeaderUtils.kafkaHeadersToMap(kafkaHeaders), vertx);
    String recordId = okapiConnectionParams.getHeaders().get(RECORD_ID_HEADER);
    OrderAuditEvent orderAuditEvent = new JsonObject(record.value()).mapTo(OrderAuditEvent.class);
    LOGGER.debug("Event was received with recordId: {} event type: {}", recordId, orderAuditEvent.getAction());

    orderAuditEventService.collectData(orderAuditEvent.getId(), OrderAuditEvent.Action.CREATE.value(), orderAuditEvent.getOrderId(),
        orderAuditEvent.getUserId(), orderAuditEvent.getEventDate(), orderAuditEvent.getActionDate(),
        orderAuditEvent.getOrderSnapshot().toString(), okapiConnectionParams.getTenantId());
    return result.future();
  }
}
