package org.folio.verticle.acquisition;

import org.folio.kafka.AsyncRecordHandler;
import org.folio.util.AcquisitionEventType;
import org.folio.verticle.AbstractConsumersVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InvoiceEventConsumersVerticle extends AbstractConsumersVerticle {

  @Autowired
  private AsyncRecordHandler<String, String> invoiceEventsHandler;

  @Override
  public List<String> getEvents() {
    return List.of(AcquisitionEventType.ACQ_INVOICE_CHANGED.getTopicName());
  }

  @Override
  public AsyncRecordHandler<String, String> getHandler() {
    return invoiceEventsHandler;
  }
}
