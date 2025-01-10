package org.folio.verticle.acquisition;

import org.folio.kafka.AsyncRecordHandler;
import org.folio.util.AcquisitionEventType;
import org.folio.verticle.AbstractConsumersVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrganizationEventConsumersVerticle extends AbstractConsumersVerticle {

  @Autowired
  private AsyncRecordHandler<String, String> organizationEventsHandler;

  @Override
  public List<String> getEvents() {
    return List.of(AcquisitionEventType.ACQ_ORGANIZATION_CHANGED.getTopicName());
  }

  @Override
  public AsyncRecordHandler<String, String> getHandler() {
    return organizationEventsHandler;
  }
}
