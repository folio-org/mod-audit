package org.folio.builder.description;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Date;
import java.util.Objects;

public class DescriptionHelper {

  private DescriptionHelper() {
  }

  private static final DateTimeFormatter FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

  public static String getFormattedDateTime(DateTime dateTime) {
    return dateTime.toString(FORMAT);
  }

  public static String getFormattedDateTime(Date date) {
    if (Objects.nonNull(date)) {
      return new DateTime(date.getTime()).toString(FORMAT);
    }
    return StringUtils.EMPTY;
  }
}
