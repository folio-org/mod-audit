package org.folio.verticle.user;

import static org.folio.util.user.UserUtils.formatUserTopicPattern;

import java.util.List;
import org.folio.kafka.AsyncRecordHandler;
import org.folio.kafka.KafkaConfig;
import org.folio.kafka.SubscriptionDefinition;
import org.folio.util.user.UserKafkaEvent;
import org.folio.verticle.AbstractConsumersVerticle;
import org.folio.verticle.user.consumers.UserEventHandler;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Component;

@Component
public class UserConsumersVerticle extends AbstractConsumersVerticle {

  private final ObjectFactory<UserEventHandler> recordHandlerProvider;

  public UserConsumersVerticle(ObjectFactory<UserEventHandler> recordHandlerProvider) {
    this.recordHandlerProvider = recordHandlerProvider;
  }

  @Override
  protected SubscriptionDefinition subscriptionDefinition(String event, KafkaConfig kafkaConfig) {
    return SubscriptionDefinition.builder()
      .eventType(event)
      .subscriptionPattern(formatUserTopicPattern(kafkaConfig.getEnvId(), UserKafkaEvent.USER))
      .build();
  }

  @Override
  public List<String> getEvents() {
    return List.of(UserKafkaEvent.USER.getTopicName());
  }

  @Override
  public AsyncRecordHandler<String, String> getHandler() {
    return recordHandlerProvider.getObject();
  }
}
