package org.folio.services.management;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class YearQuarterTest {

  private static final LocalDateTime TEST_DATE_TIME = LocalDateTime.of(2023, 5, 15, 0, 0);

  @Test
  void testCurrent() {
    var currentQuarter = YearQuarter.current(TEST_DATE_TIME);
    assertEquals(YearQuarter.Q2, currentQuarter);
  }

  @Test
  void testNext() {
    var nextQuarter = YearQuarter.next(TEST_DATE_TIME);
    assertEquals(YearQuarter.Q3, nextQuarter);
  }

  @Test
  void testFromValue() {
    assertEquals(YearQuarter.Q1, YearQuarter.fromValue(1));
    assertEquals(YearQuarter.Q2, YearQuarter.fromValue(2));
    assertEquals(YearQuarter.Q3, YearQuarter.fromValue(3));
    assertEquals(YearQuarter.Q4, YearQuarter.fromValue(4));
  }
}