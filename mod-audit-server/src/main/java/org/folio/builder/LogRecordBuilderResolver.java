package org.folio.builder;

import org.folio.builder.record.CheckInRecordBuilder;
import org.folio.builder.record.LogRecordBuilder;

public class LogRecordBuilderResolver {

  public static final String CHECK_IN_EVENT = "CHECK_IN_EVENT";
  public static final String CHECK_OUT_EVENT = "CHECK_OUT_EVENT";

  private LogRecordBuilderResolver() { }

  public static LogRecordBuilder getBuilder(String logEventType) {
    if (logEventType.equals(CHECK_IN_EVENT)) {
      return new CheckInRecordBuilder();
    } else if (logEventType.equals(CHECK_OUT_EVENT)) {
      return new CheckInRecordBuilder();
    } else {
      throw new IllegalArgumentException("Builder isn't implemented yet for: " + logEventType);
    }
  }
}
