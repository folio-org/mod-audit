package org.folio.builder.description;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class DescriptionHelper {

  private DescriptionHelper() {
  }

  private static final DateTimeFormatter FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

  public static String getFormattedDateTime(DateTime dateTime) {
    return dateTime.toString(FORMAT);
  }
}
