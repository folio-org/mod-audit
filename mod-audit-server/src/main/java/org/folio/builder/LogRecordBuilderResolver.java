package org.folio.builder;

import java.util.Map;

import org.folio.builder.service.CheckInRecordBuilder;
import org.folio.builder.service.CheckOutRecordBuilder;
import org.folio.builder.service.FeeFineRecordBuilder;
import org.folio.builder.service.LoanRecordBuilder;
import org.folio.builder.service.LogRecordBuilder;
import org.folio.builder.service.ManualBlockRecordBuilder;
import org.folio.builder.service.NoticeErrorRecordBuilder;
import org.folio.builder.service.NoticeSuccessRecordBuilder;
import org.folio.builder.service.RequestRecordBuilder;

import io.vertx.core.Context;

public class LogRecordBuilderResolver {

  public static final String CHECK_IN_EVENT = "CHECK_IN_EVENT";
  public static final String CHECK_OUT_EVENT = "CHECK_OUT_EVENT";
  public static final String CHECK_OUT_THROUGH_OVERRIDE_EVENT = "CHECK_OUT_THROUGH_OVERRIDE_EVENT";

  public static final String MANUAL_BLOCK_CREATED = "MANUAL_BLOCK_CREATED_EVENT";
  public static final String MANUAL_BLOCK_MODIFIED = "MANUAL_BLOCK_MODIFIED_EVENT";
  public static final String MANUAL_BLOCK_DELETED = "MANUAL_BLOCK_DELETED_EVENT";
  public static final String LOAN = "LOAN";
  public static final String HELD_FOR_USE_AT_LOCATION = "HELD_FOR_USE_AT_LOCATION_EVENT";
  public static final String PICKED_UP_FOR_USE_AT_LOCATION = "PICKED_UP_FOR_USE_AT_LOCATION_EVENT";
  public static final String NOTICE = "NOTICE";
  public static final String NOTICE_ERROR = "NOTICE_ERROR";
  public static final String FEE_FINE = "FEE_FINE";

  public static final String REQUEST_CREATED = "REQUEST_CREATED_EVENT";
  public static final String REQUEST_CREATED_THROUGH_OVERRIDE = "REQUEST_CREATED_THROUGH_OVERRIDE_EVENT";
  public static final String REQUEST_UPDATED = "REQUEST_UPDATED_EVENT";
  public static final String REQUEST_MOVED = "REQUEST_MOVED_EVENT";
  public static final String REQUEST_REORDERED = "REQUEST_REORDERED_EVENT";
  public static final String REQUEST_CANCELLED = "REQUEST_CANCELLED_EVENT";
  public static final String REQUEST_EXPIRED = "REQUEST_EXPIRED_EVENT";

  private LogRecordBuilderResolver() {
  }

  public static LogRecordBuilder getBuilder(String logEventType, Map<String, String> okapiHeaders, Context vertxContext) {
    switch (logEventType) {
    case CHECK_IN_EVENT:
      return new CheckInRecordBuilder(okapiHeaders, vertxContext);
    case CHECK_OUT_EVENT:
    case CHECK_OUT_THROUGH_OVERRIDE_EVENT:
      return new CheckOutRecordBuilder(okapiHeaders, vertxContext, logEventType);
    case MANUAL_BLOCK_CREATED:
    case MANUAL_BLOCK_MODIFIED:
    case MANUAL_BLOCK_DELETED:
      return new ManualBlockRecordBuilder(okapiHeaders, vertxContext);
    case LOAN:
    case HELD_FOR_USE_AT_LOCATION:
    case PICKED_UP_FOR_USE_AT_LOCATION:
      return new LoanRecordBuilder(okapiHeaders, vertxContext);
    case NOTICE:
      return new NoticeSuccessRecordBuilder(okapiHeaders, vertxContext);
    case NOTICE_ERROR:
      return new NoticeErrorRecordBuilder(okapiHeaders, vertxContext);
    case FEE_FINE:
      return new FeeFineRecordBuilder(okapiHeaders, vertxContext);
    case REQUEST_CREATED:
    case REQUEST_CREATED_THROUGH_OVERRIDE:
    case REQUEST_UPDATED:
    case REQUEST_MOVED:
    case REQUEST_REORDERED:
    case REQUEST_EXPIRED:
      return new RequestRecordBuilder(okapiHeaders, vertxContext);
    default:
      throw new IllegalArgumentException("Builder isn't implemented yet for: " + logEventType);
    }
  }
}
