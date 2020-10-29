package org.folio.builder;

import io.vertx.core.Context;
import org.folio.builder.service.CheckInRecordBuilder;
import org.folio.builder.service.CheckOutRecordBuilder;
import org.folio.builder.service.FeeFineRecordBuilder;
import org.folio.builder.service.LoanRecordBuilder;
import org.folio.builder.service.LogRecordBuilder;
import org.folio.builder.service.ManualBlockRecordBuilder;
import org.folio.builder.service.NoticeRecordBuilder;
import org.folio.builder.service.RequestRecordBuilder;

import java.util.Map;

public class LogRecordBuilderResolver {

  public static final String CHECK_IN_EVENT = "CHECK_IN_EVENT";
  public static final String CHECK_OUT_EVENT = "CHECK_OUT_EVENT";

  public static final String MANUAL_BLOCK_CREATED = "MANUAL_BLOCK_CREATED_EVENT";
  public static final String MANUAL_BLOCK_MODIFIED = "MANUAL_BLOCK_MODIFIED_EVENT";
  public static final String MANUAL_BLOCK_DELETED = "MANUAL_BLOCK_DELETED_EVENT";
  public static final String LOAN = "LOAN";
  public static final String NOTICE = "NOTICE";
  public static final String FEE_FINE = "FEE_FINE";

  public static final String REQUEST_CREATED = "REQUEST_CREATED_EVENT";
  public static final String REQUEST_UPDATED = "REQUEST_UPDATED_EVENT";
  public static final String REQUEST_MOVED = "REQUEST_MOVED_EVENT";
  public static final String REQUEST_REORDERED = "REQUEST_REORDERED_EVENT";
  public static final String REQUEST_CANCELLED = "REQUEST_CANCELLED_EVENT";

  private LogRecordBuilderResolver() {
  }

  public static LogRecordBuilder getBuilder(String logEventType, Map<String, String> okapiHeaders, Context vertxContext) {
    switch (logEventType) {
    case CHECK_IN_EVENT:
      return new CheckInRecordBuilder(okapiHeaders, vertxContext);
    case CHECK_OUT_EVENT:
      return new CheckOutRecordBuilder(okapiHeaders, vertxContext);
    case MANUAL_BLOCK_CREATED:
    case MANUAL_BLOCK_MODIFIED:
    case MANUAL_BLOCK_DELETED:
      return new ManualBlockRecordBuilder(okapiHeaders, vertxContext);
    case LOAN:
      return new LoanRecordBuilder(okapiHeaders, vertxContext);
    case NOTICE:
      return new NoticeRecordBuilder(okapiHeaders, vertxContext);
    case FEE_FINE:
      return new FeeFineRecordBuilder(okapiHeaders, vertxContext);
    case REQUEST_CREATED:
    case REQUEST_UPDATED:
    case REQUEST_MOVED:
    case REQUEST_REORDERED:
      return new RequestRecordBuilder(okapiHeaders, vertxContext);
    default:
      throw new IllegalArgumentException("Builder isn't implemented yet for: " + logEventType);
    }
  }
}
