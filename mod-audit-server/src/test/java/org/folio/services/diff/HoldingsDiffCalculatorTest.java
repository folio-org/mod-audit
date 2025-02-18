package org.folio.services.diff;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import org.folio.CopilotGenerated;
import org.folio.domain.diff.FieldChangeDto;
import org.folio.rest.external.HoldingsRecord;
import org.folio.util.inventory.InventoryResourceType;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
@CopilotGenerated
class HoldingsDiffCalculatorTest {

  private final HoldingsDiffCalculator holdingsDiffCalculator = new HoldingsDiffCalculator();

  @Test
  void shouldReturnHoldingsResourceType() {
    assertThat(holdingsDiffCalculator.getResourceType()).isEqualTo(InventoryResourceType.HOLDINGS);
  }

  @Test
  void shouldInitializeInnerObjectFields() {
    // given
    var holdingsRecord = new HoldingsRecord();

    // when
    var result = holdingsDiffCalculator.access(holdingsRecord).get();

    // then
    assertThat(result.getTags()).isNotNull();
    assertThat(result.getReceivingHistory()).isNotNull();
  }

  @Test
  void shouldDetectFieldModifiedChange() {
    // given
    var oldHoldingsRecord = getMap(new HoldingsRecord().withId("1").withCallNumber("Call Number 1"));
    var newHoldingsRecord = getMap(new HoldingsRecord().withId("1").withCallNumber("Call Number 2"));

    // when
    var changeRecordDTO = holdingsDiffCalculator.calculateDiff(oldHoldingsRecord, newHoldingsRecord);

    // then
    assertThat(changeRecordDTO.getFieldChanges())
      .as("Field changes should contain call number field modified change")
      .hasSize(1)
      .containsExactlyInAnyOrder(FieldChangeDto.modified("callNumber", "callNumber", "Call Number 1", "Call Number 2"));
  }

  private static Map<String, Object> getMap(HoldingsRecord obj) {
    return new JsonObject(Json.encode(obj)).getMap();
  }
}