package org.folio.rest.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CirculationLogsServiceTest {

  @ParameterizedTest
  @MethodSource("getCQLAndFieldsData")
  void test_checkCqlFieldsAndSortConditions(String cqlQuery, List<String> fieldNames, List<String> negativeFieldNames, List<String> sortFieldsNames, Boolean expectedResult) {
    Boolean isConditionExist = CirculationLogsService.checkCqlFieldsAndSortConditions(cqlQuery, fieldNames, negativeFieldNames, sortFieldsNames);
    assertThat(isConditionExist, equalTo(expectedResult));
  }

  private static Stream<Arguments> getCQLAndFieldsData() {
    return Stream.of(
      // Scenario 1: Simple query with only required field
      Arguments.of("(items=\"ITEM_BARCODE_256069\")", List.of("items"), null, null, true),

      // Scenario 2: Query with missing required field
      Arguments.of("(userBarcode=\"USER123\")", List.of("items"), null, null, false),

      // Scenario 3: Complex query with date range, multiple actions, and valid sort
      Arguments.of(
        "(date>=\"2024-06-01T00:00:00.000Z\" and date<=\"2025-06-23T23:59:59.999Z\" and items=\"QQQ\" and action==(\"Changed due date\" or \"Billed\" or \"Send\" or \"Created\")) sortby date/sort.descending",
        List.of("items"), List.of("description", "userBarcode"), List.of("date"), true),

      // Scenario 4: Query with negative field present (userBarcode)
      Arguments.of("(items=\"ITEM_BARCODE_153009\" and userBarcode==\"*USER_BARCODE_153009*\") sortby date/sort.descending",
        List.of("items"), List.of("description", "userBarcode"), List.of("date"), false),

      // Scenario 5: Query with negative field present (description)
      Arguments.of("(description==\"*DESC*\" and items=\"ITEM_BARCODE_256069\") sortby date/sort.descending", List.of("items"),
        List.of("description", "userBarcode"), List.of("date"), false),

      // Scenario 6: Query with invalid sort field
      Arguments.of("(items=\"ITEM_123\") sortby userBarcode/sort.descending", List.of("items"), null, List.of("date"), false),

      // Scenario 7: Query with null negative fields but valid sort
      Arguments.of("(items=\"ITEM_BARCODE_256069\" and action==(\"Created\")) sortby date/sort.descending", List.of("items"), null,
        List.of("date"), true),

      // Scenario 8: Query with all null parameters
      Arguments.of("(items=\"ITEM_123\")", null, null, null, true),

      // Scenario 9: Query with empty lists
      Arguments.of("(items=\"ITEM_123\")", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), true),

      // Scenario 10: Empty query string
      Arguments.of("", List.of("items"), List.of("description"), List.of("date"), false),

      // Scenario 11: Complex query with multiple actions and valid sort
      Arguments.of("(items=\"ITEM_123\" and action==(\"Created\" or \"Updated\")) sortby date/sort.descending",
        List.of("items", "action"), List.of("description"), List.of("date"), true),

      // Scenario 12: Query without sort, testing field presence
      Arguments.of(
        "(date>=\"2024-06-01T00:00:00.000Z\" and date<=\"2025-06-23T23:59:59.999Z\" and items=\"QQQ\" and action==(\"Changed due date\" or \"Billed\" or \"Send\" or \"Created\"))",
        List.of("items"), List.of("description", "userBarcode"), List.of("date"), false),

      // Scenario 13: Query with null sort fields
      Arguments.of("(items=\"ITEM_BARCODE_256069\" and action==(\"Created\")) sortby date/sort.descending", List.of("items"), null,
        null, true),
      // Scenario 14: More scenarios
      Arguments.of("(date>=\"2024-06-01T00:00:00.000Z\" and date<=\"2025-06-23T23:59:59.999Z\" and items=\"QQQ\" and action==(\"Changed due date\" or \"Billed\" or \"Send\" or \"Created\"))", List.of("items"), List.of("description", "userBarcode"), List.of("date"), false),
      Arguments.of("(date>=\"2024-06-01T00:00:00.000Z\" and date<=\"2025-06-23T23:59:59.999Z\" and items=\"ITEM_BARCODE_256069\") sortby date/sort.descending", List.of("items"), List.of("description", "userBarcode"), List.of("date"), true),
      Arguments.of("(date>=\"2024-06-01T00:00:00.000Z\" and date<=\"2025-06-23T23:59:59.999Z\" and items=\"QQQ\" and action==(\"Changed due date\" or \"Billed\" or \"Send\" or \"Created\")) sortby date/sort.descending", List.of("items"), List.of("description", "userBarcode"), List.of("date"), true),
      Arguments.of("(items=\"ITEM_BARCODE_153009\" and userBarcode==\"*USER_BARCODE_153009*\") sortby date/sort.descending", List.of("items"), List.of("description", "userBarcode"), List.of("date"), false),
      Arguments.of("(description==\"*DESC*\" and items=\"ITEM_BARCODE_256069\") sortby date/sort.descending", List.of("items"), List.of("description", "userBarcode"), List.of("date"), false),
      Arguments.of("(description==\"*DESC*\" and items=\"ITEM_BARCODE_256069\" and action==(\"Send\")) sortby date/sort.descending", List.of("items"), List.of("description", "userBarcode"), List.of("date"), false),
      Arguments.of("(items=\"ITEM_BARCODE_256069\" and action==(\"Created\")) sortby date/sort.descending", List.of("items"), List.of("description", "userBarcode"), List.of("date"), true),
      Arguments.of("(items=\"ITEM_BARCODE_256069\" and action==(\"Created\")) sortby date/sort.descending", List.of("items"), null, List.of("date"), true),
      Arguments.of("(items=\"ITEM_BARCODE_256069\" and action==(\"Created\")) sortby date/sort.descending", List.of("items"), null, List.of("action"), false),
      Arguments.of("(items=\"ITEM_BARCODE_256069\" and action==(\"Created\")) sortby date/sort.descending", List.of("items"), null, null, true),
      Arguments.of("(items=\"ITEM_123\") sortby userBarcode/sort.descending", List.of("items"), null, List.of("date"), false),
      Arguments.of("(items=\"ITEM_123\")", null, null, null, true),
      Arguments.of("(items=\"ITEM_123\")", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), true),
      Arguments.of("", List.of("items"),  List.of("description"), List.of("date"), false),
      Arguments.of("(items=\"ITEM_BARCODE_256069\")", List.of("items"),  null, null, true),
      Arguments.of("(items=\"ITEM_BARCODE_256069\")", List.of("items"),  null, null, true)
    );
  }
}