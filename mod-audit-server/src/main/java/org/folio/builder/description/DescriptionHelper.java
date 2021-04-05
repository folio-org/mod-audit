package org.folio.builder.description;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

public class DescriptionHelper {

  private DescriptionHelper() {
  }

  public static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
  private static final DateTimeFormatter JODA_TIME_FORMATTER = DateTimeFormat.forPattern(DATE_PATTERN);
  private static final java.time.format.DateTimeFormatter DATE_TIME_FORMATTER = java.time.format.DateTimeFormatter.ofPattern(DATE_PATTERN);

  public static String getFormattedDateTime(DateTime dateTime) {
    return dateTime.toString(JODA_TIME_FORMATTER);
  }

  public static String getFormattedDateTime(Date date) {
    if (Objects.nonNull(date)) {
      return LocalDateTime.ofInstant(date.toInstant(), ZoneId.of("GMT"))
        .atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMATTER);
    }
    return StringUtils.EMPTY;
  }
}
