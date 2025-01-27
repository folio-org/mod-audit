package org.folio.verticle.inventory.consumers;

import static org.folio.util.inventory.InventoryEventType.CREATE;
import static org.folio.util.inventory.InventoryEventType.DELETE;
import static org.folio.util.inventory.InventoryEventType.UPDATE;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.kafka.AsyncRecordHandler;
import org.folio.kafka.KafkaHeaderUtils;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.inventory.InventoryEventService;
import org.folio.util.KafkaUtils;
import org.folio.util.inventory.InventoryEvent;
import org.folio.util.inventory.InventoryKafkaEvent;
import org.folio.util.inventory.InventoryResourceType;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventHandler implements AsyncRecordHandler<String, String> {

  private static final Logger LOGGER = LogManager.getLogger();
  private static final Map<String, InventoryResourceType> TOPIC_TO_RESOURCE_MAP = Map.ofEntries(
    Map.entry(InventoryKafkaEvent.INSTANCE.getTopicName(), InventoryResourceType.INSTANCE),
    Map.entry(InventoryKafkaEvent.HOLDINGS.getTopicName(), InventoryResourceType.HOLDINGS),
    Map.entry(InventoryKafkaEvent.ITEM.getTopicName(), InventoryResourceType.ITEM)
  );

  private final InventoryEventService inventoryEventService;
  private final Vertx vertx;

  public InventoryEventHandler(Vertx vertx, InventoryEventService inventoryEventService) {
    this.vertx = vertx;
    this.inventoryEventService = inventoryEventService;
  }

  @Override
  public Future<String> handle(KafkaConsumerRecord<String, String> kafkaConsumerRecord) {
    var result = Promise.<String>promise();
    var kafkaHeaders = kafkaConsumerRecord.headers();
    var okapiConnectionParams = new OkapiConnectionParams(KafkaHeaderUtils.kafkaHeadersToMap(kafkaHeaders), vertx);
    var event = constructInventoryEvent(kafkaConsumerRecord);
    if (!isSupportedEvent(event)) {
      LOGGER.debug("handle:: Event not supported [type: {}, eventId: {}, entityId: {}]",
        event.getType(), event.getEventId(), event.getEntityId());
      result.complete(event.getEventId());
      return result.future();
    }

    LOGGER.info("handle:: Starting processing of Inventory event with id: {} for entity id: {}", event.getEventId(), event.getEntityId());
    inventoryEventService.saveEvent(event, okapiConnectionParams.getTenantId())
      .onSuccess(ar -> {
        LOGGER.info("handle:: Inventory event with id: {} has been processed for entity id: {}", event.getEventId(), event.getEntityId());
        result.complete(event.getEventId());
      })
      .onFailure(e -> {
        if (e instanceof DuplicateEventException) {
          LOGGER.info("handle:: Duplicate Inventory event with id: {} for entity id: {} received, skipped processing", event.getEventId(), event.getEntityId());
          result.complete(event.getEventId());
        } else {
          LOGGER.error("Processing of Inventory event with id: {} for entity id: {} has been failed", event.getEventId(), event.getEntityId(), e);
          result.fail(e);
        }
      });
    return result.future();
  }

  private InventoryEvent constructInventoryEvent(KafkaConsumerRecord<String, String> consumerRecord) {
    var topicName = KafkaUtils.getTopicName(consumerRecord);
    var resourceType = TOPIC_TO_RESOURCE_MAP.getOrDefault(topicName, InventoryResourceType.UNKNOWN);
    var entityId = consumerRecord.key();

    var event = new JsonObject(consumerRecord.value()).mapTo(InventoryEvent.class);
    event.setEntityId(entityId);
    event.setResourceType(resourceType);
    return event;
  }

  private boolean isSupportedEvent(InventoryEvent event) {
    return CREATE == event.getType() || UPDATE == event.getType() || DELETE == event.getType();
  }
}