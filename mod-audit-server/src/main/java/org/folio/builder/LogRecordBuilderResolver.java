package org.folio.builder;

import org.folio.builder.service.CheckInRecordBuilderService;
import org.folio.builder.service.CheckOutRecordBuilderService;
import org.folio.builder.service.FeeFineRecordBuilderService;
import org.folio.builder.service.LoanRecordBuilderService;
import org.folio.builder.service.LogRecordBuilderService;
import org.folio.builder.service.ManualBlockRecordBuilderService;
import org.folio.builder.service.NoticeRecordBuilderService;
import org.folio.builder.service.RequestRecordBuilderService;

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

  public static LogRecordBuilderService getBuilder(String logEventType) {
    switch (logEventType) {
    case CHECK_IN_EVENT:
      return new CheckInRecordBuilderService();
    case CHECK_OUT_EVENT:
      return new CheckOutRecordBuilderService();
    case MANUAL_BLOCK_CREATED:
    case MANUAL_BLOCK_MODIFIED:
    case MANUAL_BLOCK_DELETED:
      return new ManualBlockRecordBuilderService();
    case LOAN:
      return new LoanRecordBuilderService();
    case NOTICE:
      return new NoticeRecordBuilderService();
    case FEE_FINE:
      return new FeeFineRecordBuilderService();
    case REQUEST_CREATED:
    case REQUEST_UPDATED:
    case REQUEST_MOVED:
    case REQUEST_REORDERED:
      return new RequestRecordBuilderService();
    default:
      throw new IllegalArgumentException("Builder isn't implemented yet for: " + logEventType);
    }
  }
}
