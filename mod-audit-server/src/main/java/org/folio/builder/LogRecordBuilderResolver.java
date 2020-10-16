package org.folio.builder;

import io.vertx.core.Context;
import org.folio.builder.service.CheckInRecordBuilderService;
import org.folio.builder.service.CheckOutRecordBuilderService;
import org.folio.builder.service.FeeFineRecordBuilderService;
import org.folio.builder.service.LoanRecordBuilderService;
import org.folio.builder.service.LogRecordBuilderService;
import org.folio.builder.service.ManualBlockRecordBuilderService;
import org.folio.builder.service.NoticeRecordBuilderService;

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

  private LogRecordBuilderResolver() {
  }

  public static LogRecordBuilderService getBuilder(String logEventType, Context context, Map<String, String> headers) {
    switch (logEventType) {
    case CHECK_IN_EVENT:
      return new CheckInRecordBuilderService(context, headers);
    case CHECK_OUT_EVENT:
      return new CheckOutRecordBuilderService(context, headers);
    case MANUAL_BLOCK_CREATED:
    case MANUAL_BLOCK_MODIFIED:
    case MANUAL_BLOCK_DELETED:
      return new ManualBlockRecordBuilderService(context, headers);
    case LOAN:
      return new LoanRecordBuilderService(context, headers);
    case NOTICE:
      return new NoticeRecordBuilderService(context, headers);
    case FEE_FINE:
      return new FeeFineRecordBuilderService(context, headers);
    default:
      throw new IllegalArgumentException("Builder isn't implemented yet for: " + logEventType);
    }
  }
}
