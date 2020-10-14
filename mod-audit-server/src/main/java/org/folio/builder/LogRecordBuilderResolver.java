package org.folio.builder;

import org.folio.builder.record.CheckInRecordBuilder;
import org.folio.builder.record.CheckOutRecordBuilder;
import org.folio.builder.record.LogRecordBuilder;
import org.folio.builder.record.ManualBlockRecordBuilder;

public class LogRecordBuilderResolver {

  public static final String CHECK_IN_EVENT = "CHECK_IN_EVENT";
  public static final String CHECK_OUT_EVENT = "CHECK_OUT_EVENT";
  public static final String MANUAL_BLOCK_CREATED = "MANUAL_BLOCK_CREATED_EVENT";
  public static final String MANUAL_BLOCK_MODIFIED = "MANUAL_BLOCK_MODIFIED_EVENT";
  public static final String MANUAL_BLOCK_DELETED = "MANUAL_BLOCK_DELETED_EVENT";

  private LogRecordBuilderResolver() {
  }

  public static LogRecordBuilder getBuilder(String logEventType) {
    switch (logEventType) {
    case CHECK_IN_EVENT:
      return new CheckInRecordBuilder();
    case CHECK_OUT_EVENT:
      return new CheckOutRecordBuilder();
    case MANUAL_BLOCK_CREATED:
    case MANUAL_BLOCK_MODIFIED:
    case MANUAL_BLOCK_DELETED:
      return new ManualBlockRecordBuilder();
    default:
      throw new IllegalArgumentException("Builder isn't implemented yet for: " + logEventType);
    }
  }
}
