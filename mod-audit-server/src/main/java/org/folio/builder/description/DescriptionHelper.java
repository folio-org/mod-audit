package org.folio.builder.description;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class DescriptionHelper {

  private DescriptionHelper() {
  }

  private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
  private static final DateTimeFormatter FORMAT = DateTimeFormat.forPattern(DATE_TIME_PATTERN);
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  public static String getFormattedDateTime(DateTime dateTime) {
    return dateTime.toString(FORMAT);
  }

  public static String getFormattedDateTime(Date date) {
    if (Objects.nonNull(date)) {
      return DATE_FORMAT.format(date);
    }
    return StringUtils.EMPTY;
  }
}
