package org.folio.services.management;

import java.time.LocalDateTime;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DatabaseSubPartition {

  private static final String FORMAT = "%s_%s_%s_q%s";

  private final String table;
  private final String partition;
  private final int year;
  private final YearQuarter quarter;
  private final String fullName;

  public DatabaseSubPartition(String tableName, int partitionNumber, int year, YearQuarter yearQuarter) {
    this.table = tableName;
    this.partition = "p" + partitionNumber;
    this.year = year;
    this.quarter = yearQuarter;
    this.fullName = null;
  }

  public static DatabaseSubPartition fromString(String partition) {
    var parts = partition.split("_");
    // Table name may contain two/three words separated by underscore so we need to parse from the end
    var yearQuarter = YearQuarter.fromValue(Integer.parseInt(parts[parts.length - 1].substring(1)));
    var yearInt = Integer.parseInt(parts[parts.length - 2]);
    var partitionValue = parts[parts.length - 3];
    var tableName = Arrays.stream(Arrays.copyOfRange(parts, 0, parts.length - 3))
      .reduce((a, b) -> a + "_" + b)
      .orElse("");
    return new DatabaseSubPartition(tableName, partitionValue, yearInt, yearQuarter, partition);
  }

  @Override
  public String toString() {
    return fullName != null ? fullName : FORMAT.formatted(table, partition, year, quarter.getValue());
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj)
      || (obj instanceof DatabaseSubPartition other && this.toString().equals(other.toString()));
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  public boolean isBefore(LocalDateTime dateTime) {
    var otherYear = dateTime.getYear();
    return year < otherYear || (year == otherYear && quarter.getValue() < getQuarterValue(dateTime));
  }

  public boolean isCurrent(LocalDateTime dateTime) {
    return year == dateTime.getYear() && quarter.getValue() == getQuarterValue(dateTime);
  }

  public String getMainPartition() {
    return table + "_" + partition;
  }

  private static int getQuarterValue(LocalDateTime dateTime) {
    return (int) Math.ceil(dateTime.getMonthValue() / 3.0);
  }
}
