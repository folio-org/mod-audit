package org.folio.builder;

import org.folio.builder.record.CheckInRecordBuilder;
import org.folio.builder.record.CheckOutRecordBuilder;
import org.folio.builder.record.LogRecordBuilder;
import org.folio.builder.record.ManualBlockRecordBuilder;
import org.folio.builder.record.RequestRecordBuilder;

public class LogRecordBuilderResolver {

  public static final String CHECK_IN_EVENT = "CHECK_IN_EVENT";
  public static final String CHECK_OUT_EVENT = "CHECK_OUT_EVENT";

  public static final String MANUAL_BLOCK_CREATED = "MANUAL_BLOCK_CREATED_EVENT";
  public static final String MANUAL_BLOCK_MODIFIED = "MANUAL_BLOCK_MODIFIED_EVENT";
  public static final String MANUAL_BLOCK_DELETED = "MANUAL_BLOCK_DELETED_EVENT";

  public static final String REQUEST_CREATED = "REQUEST_CREATED_EVENT";
  public static final String REQUEST_UPDATED = "REQUEST_UPDATED_EVENT";
  public static final String REQUEST_MOVED = "REQUEST_MOVED_EVENT";
  public static final String REQUEST_REORDERED = "REQUEST_REORDERED_EVENT";
  public static final String REQUEST_CANCELLED = "REQUEST_CANCELLED_EVENT";

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
    case REQUEST_CREATED:
    case REQUEST_UPDATED:
    case REQUEST_MOVED:
    case REQUEST_REORDERED:
      return new RequestRecordBuilder();
    default:
      throw new IllegalArgumentException("Builder isn't implemented yet for: " + logEventType);
    }
  }
}
