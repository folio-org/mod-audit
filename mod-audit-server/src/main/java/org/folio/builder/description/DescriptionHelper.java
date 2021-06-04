package org.folio.builder.description;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;

public class DescriptionHelper {

  private DescriptionHelper() {
  }

  public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
  public static final String DATE_PATTERN = "yyyy-MM-dd";
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
  private static final java.time.format.DateTimeFormatter DATE_FORMATTER = java.time.format.DateTimeFormatter.ofPattern(DATE_PATTERN);

  public static String getFormattedDateTime(LocalDateTime dateTime) {
    return dateTime.format(DATE_TIME_FORMATTER);
  }

  public static String getFormattedDateTime(Date date) {
    if (Objects.nonNull(date)) {
      return LocalDateTime.ofInstant(date.toInstant(), ZoneId.of("GMT"))
        .atZone(ZoneId.systemDefault()).format(DATE_FORMATTER);
    }
    return StringUtils.EMPTY;
  }
}
