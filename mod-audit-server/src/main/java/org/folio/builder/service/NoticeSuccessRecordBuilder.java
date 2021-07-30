package org.folio.builder.service;

import java.util.Map;

import org.folio.rest.jaxrs.model.LogRecord;

import io.vertx.core.Context;

public class NoticeSuccessRecordBuilder extends AbstractNoticeRecordBuilder {

  public NoticeSuccessRecordBuilder(Map<String, String> okapiHeaders, Context vertxContext) {
    super(okapiHeaders, vertxContext, LogRecord.Action.SEND);
  }

}
