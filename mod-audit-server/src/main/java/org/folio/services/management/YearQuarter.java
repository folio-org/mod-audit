package org.folio.services.management;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public enum YearQuarter {

  Q1(1, 1, 4),
  Q2(2, 4, 7),
  Q3(3, 7, 10),
  Q4(4, 10, 1);

  private final int value;
  private final int monthFrom;
  private final int monthTo;

  YearQuarter(int value, int monthFrom, int monthTo) {
    this.value = value;
    this.monthFrom = monthFrom;
    this.monthTo = monthTo;
  }

  public static YearQuarter current(LocalDateTime dateTime) {
    var quarterValue = valueForDate(dateTime);
    return fromValue(quarterValue);
  }

  public static YearQuarter next(LocalDateTime dateTime) {
    var quarterValue = valueForDate(dateTime);
    quarterValue = quarterValue == 4 ? 1 : quarterValue + 1;
    return fromValue(quarterValue);
  }

  public static YearQuarter fromValue(int value) {
    return YearQuarter.values()[value - 1];
  }

  private static int valueForDate(LocalDateTime dateTime) {
    return (int) Math.ceil(dateTime.getMonthValue() / 3.0);
  }
}
