package org.folio.services.management;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.folio.CopilotGenerated;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
@CopilotGenerated
class DatabaseSubPartitionTest {

  @Test
  void testFromString() {
    var partitionString = "table_p1_2023_q2";
    var subPartition = DatabaseSubPartition.fromString(partitionString);

    assertEquals("table", subPartition.getTable());
    assertEquals("p1", subPartition.getPartition());
    assertEquals(2023, subPartition.getYear());
    assertEquals(YearQuarter.Q2, subPartition.getQuarter());
    assertEquals(partitionString, subPartition.toString());
  }

  @Test
  void testToString() {
    var subPartition = new DatabaseSubPartition("table", 1, 2023, YearQuarter.Q2);
    assertEquals("table_p1_2023_q2", subPartition.toString());
  }

  @Test
  void testEquals() {
    var subPartition1 = new DatabaseSubPartition("table", 1, 2023, YearQuarter.Q2);
    var subPartition2 = new DatabaseSubPartition("table", 1, 2023, YearQuarter.Q2);
    var subPartition3 = new DatabaseSubPartition("table", 2, 2023, YearQuarter.Q2);

    assertEquals(subPartition1, subPartition2);
    assertNotEquals(subPartition1, subPartition3);
    assertEquals(subPartition1, subPartition2);
  }

  @Test
  void testIsBefore() {
    var subPartition = new DatabaseSubPartition("table", 1, 2023, YearQuarter.Q2);
    var dateTime = LocalDateTime.of(2023, 5, 15, 0, 0);

    assertTrue(subPartition.isBefore(dateTime.plusMonths(3)));
    assertFalse(subPartition.isBefore(dateTime.minusMonths(3)));
  }

  @Test
  void testIsCurrent() {
    var subPartition = new DatabaseSubPartition("table", 1, 2023, YearQuarter.Q2);
    var dateTime = LocalDateTime.of(2023, 5, 15, 0, 0);

    assertTrue(subPartition.isCurrent(dateTime));
    assertFalse(subPartition.isCurrent(dateTime.plusMonths(3)));
  }

  @Test
  void testGetMainPartition() {
    var subPartition = new DatabaseSubPartition("table", 1, 2023, YearQuarter.Q2);
    assertEquals("table_p1", subPartition.getMainPartition());
  }
}