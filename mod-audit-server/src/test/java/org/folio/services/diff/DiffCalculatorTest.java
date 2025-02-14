package org.folio.services.diff;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.folio.domain.diff.ChangeType;
import org.folio.domain.diff.CollectionChangeDto;
import org.folio.domain.diff.CollectionItemChangeDto;
import org.folio.domain.diff.FieldChangeDto;
import org.folio.rest.external.Dates;
import org.folio.rest.external.Instance;
import org.folio.rest.external.Subject;
import org.folio.util.inventory.InventoryResourceType;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@UnitTest
class DiffCalculatorTest {

  private DiffCalculator<Instance> diffCalculator;

  @BeforeEach
  void setUp() {
    diffCalculator = new DiffCalculator<>() {
      @Override
      protected Class<Instance> getType() {
        return Instance.class;
      }


      @Override
      public InventoryResourceType getResourceType() {
        return InventoryResourceType.INSTANCE;
      }

      @Override
      protected Supplier<Instance> access(Instance value) {
        return () -> {
          if (value.getDates() == null) {
            value.setDates(new Dates());
          }
          return value;
        };
      }
    };
  }

  @Test
  void shouldNotDetectFieldModifiedChangeIfFieldMissingInSchema() {
    // given
    var oldInstance = Map.of("id", "1", "metadata", Map.of("createdDate", "2021-01-01"));
    var newInstance = Map.of("id", "1", "metadata", Map.of("createdDate", "2021-01-02"));

    // when
    var changeRecordDTO = diffCalculator.calculateDiff(oldInstance, newInstance);

    // then
    assertThat(changeRecordDTO.getFieldChanges())
      .as("Field changes should be empty")
      .isEmpty();
  }

  @Test
  void shouldDetectFieldModifiedChange() {
    // given
    var oldInstance = getMap(new Instance().withId("1").withTitle("Title 1"));
    var newInstance = getMap(new Instance().withId("1").withTitle("Title 2"));

    // when
    var changeRecordDTO = diffCalculator.calculateDiff(oldInstance, newInstance);

    // then
    assertThat(changeRecordDTO.getFieldChanges())
      .as("Field changes should contain title field modified change")
      .hasSize(1)
      .containsExactlyInAnyOrder(FieldChangeDto.modified("title", "title", "Title 1", "Title 2"));
  }

  @Test
  void shouldDetectFieldAddedChange() {
    // given
    var oldInstance = getMap(new Instance().withId("1").withTitle("Title 1"));
    var newInstance = getMap(new Instance().withId("1").withTitle("Title 1").withSource("Source 1"));

    // when
    var changeRecordDTO = diffCalculator.calculateDiff(oldInstance, newInstance);

    // then
    assertThat(changeRecordDTO.getFieldChanges())
      .as("Field changes should contain source field added change")
      .hasSize(1)
      .containsExactlyInAnyOrder(FieldChangeDto.added("source", "source", "Source 1"));
  }

  @Test
  void shouldDetectFieldAddedChangeInInnerObject() {
    // given
    var oldInstance = getMap(new Instance().withId("1"));
    var newInstance = getMap(new Instance().withId("1").withDates(new Dates().withDate1("Date1").withDate2("Date2")));

    // when
    var changeRecordDTO = diffCalculator.calculateDiff(oldInstance, newInstance);

    // then
    assertThat(changeRecordDTO.getFieldChanges())
      .as("Field changes should contain added changes in inner object")
      .hasSize(2)
      .containsExactlyInAnyOrder(
        FieldChangeDto.added("date1", "dates.date1", "Date1"),
        FieldChangeDto.added("date2", "dates.date2", "Date2")
      );
  }

  @Test
  void shouldDetectFieldRemovedChange() {
    // given
    var oldInstance = getMap(new Instance().withId("1").withTitle("Title 1").withSource("Source 1"));
    var newInstance = getMap(new Instance().withId("1").withTitle("Title 1"));

    // when
    var changeRecordDTO = diffCalculator.calculateDiff(oldInstance, newInstance);

    // then
    assertThat(changeRecordDTO.getFieldChanges())
      .as("Field changes should contain source field removed change")
      .hasSize(1)
      .containsExactlyInAnyOrder(FieldChangeDto.removed("source", "source", "Source 1"));
  }

  @Test
  void shouldDetectSeveralFieldChanges() {
    // given
    var oldInstance = getMap(new Instance().withId("1").withTitle("Title 1").withSource("Source 1").withHrid("Hrid 1"));
    var newInstance =
      getMap(new Instance().withId("1").withTitle("Title 2").withSource("Source 2").withStatusId("Status 2"));

    // when
    var changeRecordDTO = diffCalculator.calculateDiff(oldInstance, newInstance);

    // then
    assertThat(changeRecordDTO.getFieldChanges())
      .as("Field changes should contain several changes")
      .hasSize(4)
      .containsExactlyInAnyOrder(
        FieldChangeDto.modified("title", "title", "Title 1", "Title 2"),
        FieldChangeDto.modified("source", "source", "Source 1", "Source 2"),
        FieldChangeDto.removed("hrid", "hrid", "Hrid 1"),
        FieldChangeDto.added("statusId", "statusId", "Status 2")
      );
  }

  @Test
  void shouldDetectSeveralFieldChangesInInnerObject() {
    // given
    var oldInstance = getMap(new Instance().withId("1").withDates(new Dates().withDate1("Date 1").withDate2("Date 2")));
    var newInstance =
      getMap(new Instance().withId("1").withDates(new Dates().withDate1("Date new").withDateTypeId("Date type")));

    // when
    var changeRecordDTO = diffCalculator.calculateDiff(oldInstance, newInstance);

    // then
    assertThat(changeRecordDTO.getFieldChanges())
      .as("Field changes should contain several changes in inner object")
      .hasSize(3)
      .containsExactlyInAnyOrder(
        FieldChangeDto.modified("date1", "dates.date1", "Date 1", "Date new"),
        FieldChangeDto.removed("date2", "dates.date2", "Date 2"),
        FieldChangeDto.added("dateTypeId", "dates.dateTypeId", "Date type")
      );
  }

  @Test
  void shouldDetectFieldChangesInInnerScalarCollection() {
    // given
    var oldInstance = getMap(new Instance().withId("1").withLanguages(List.of("Language 1", "Language 2")));
    var newInstance =
      getMap(new Instance().withId("1").withLanguages(List.of("Language 3", "Language 2", "Language 4")));

    // when
    var changeRecordDTO = diffCalculator.calculateDiff(oldInstance, newInstance);

    // then
    assertThat(changeRecordDTO.getCollectionChanges())
      .as("Field changes should contain changes in inner collection")
      .hasSize(1)
      .extracting(CollectionChangeDto::getCollectionName)
      .containsExactly("languages");

    assertThat(changeRecordDTO.getCollectionChanges().get(0).getItemChanges())
      .as("Field changes should contain changes in inner collection")
      .hasSize(3)
      .containsExactlyInAnyOrder(
        CollectionItemChangeDto.removed("Language 1"),
        CollectionItemChangeDto.added("Language 3"),
        CollectionItemChangeDto.added("Language 4")
      );
  }

  @Test
  void shouldDetectFieldChangesInInnerObjectCollection() {
    // given
    var oldInstance = getMap(new Instance().withId("1")
      .withSubjects(List.of(
        new Subject().withValue("Subject 1"),
        new Subject().withValue("Subject 2").withTypeId("Type 2"),
        new Subject().withValue("Subject 3").withTypeId("Type 3")
      )));
    var newInstance = getMap(new Instance().withId("1")
      .withSubjects(List.of(
        new Subject().withValue("Subject 2").withTypeId("Type 2"),
        new Subject().withValue("Subject 3").withSourceId("Source 3"),
        new Subject().withValue("Subject 4")
      )));

    // when
    var changeRecordDTO = diffCalculator.calculateDiff(oldInstance, newInstance);

    // then
    assertThat(changeRecordDTO.getCollectionChanges())
      .as("Field changes should contain changes in inner collection")
      .hasSize(1)
      .extracting(CollectionChangeDto::getCollectionName)
      .containsExactly("subjects");

    assertThat(changeRecordDTO.getCollectionChanges().get(0).getItemChanges())
      .as("Field changes should contain changes in inner collection")
      .hasSize(4)
      .containsExactlyInAnyOrder(
        CollectionItemChangeDto.of(ChangeType.REMOVED, new Subject().withValue("Subject 1"), null),
        CollectionItemChangeDto.of(ChangeType.REMOVED, new Subject().withValue("Subject 3").withTypeId("Type 3"),
          null),
        CollectionItemChangeDto.of(ChangeType.ADDED, null,
          new Subject().withValue("Subject 3").withSourceId("Source 3")),
        CollectionItemChangeDto.of(ChangeType.ADDED, null, new Subject().withValue("Subject 4"))
      );
  }

  @Test
  void shouldDetectFieldChangesInInnerObjectCollectionAndInnerObject() {
    // given
    var oldInstance = getMap(new Instance().withId("1").withDates(new Dates().withDate1("Date 1").withDate2("Date 2"))
      .withSubjects(List.of(
        new Subject().withValue("Subject 1"),
        new Subject().withValue("Subject 2").withTypeId("Type 2"),
        new Subject().withValue("Subject 3").withTypeId("Type 3")
      )));
    var newInstance =
      getMap(new Instance().withId("1").withDates(new Dates().withDate1("Date new").withDateTypeId("Date type"))
        .withSubjects(List.of(
          new Subject().withValue("Subject 2").withTypeId("Type 2"),
          new Subject().withValue("Subject 3").withSourceId("Source 3"),
          new Subject().withValue("Subject 4")
        )));

    // when
    var changeRecordDTO = diffCalculator.calculateDiff(oldInstance, newInstance);

    // then
    assertThat(changeRecordDTO.getCollectionChanges())
      .as("Field changes should contain changes in inner collection")
      .hasSize(1)
      .extracting(CollectionChangeDto::getCollectionName)
      .containsExactly("subjects");

    assertThat(changeRecordDTO.getCollectionChanges().get(0).getItemChanges())
      .as("Field changes should contain changes in inner collection")
      .hasSize(4)
      .containsExactlyInAnyOrder(
        CollectionItemChangeDto.of(ChangeType.REMOVED, new Subject().withValue("Subject 1"), null),
        CollectionItemChangeDto.of(ChangeType.REMOVED, new Subject().withValue("Subject 3").withTypeId("Type 3"),
          null),
        CollectionItemChangeDto.of(ChangeType.ADDED, null,
          new Subject().withValue("Subject 3").withSourceId("Source 3")),
        CollectionItemChangeDto.of(ChangeType.ADDED, null, new Subject().withValue("Subject 4"))
      );

    assertThat(changeRecordDTO.getFieldChanges())
      .as("Field changes should contain several changes in inner object")
      .hasSize(3)
      .containsExactlyInAnyOrder(
        FieldChangeDto.modified("date1", "dates.date1", "Date 1", "Date new"),
        FieldChangeDto.removed("date2", "dates.date2", "Date 2"),
        FieldChangeDto.added("dateTypeId", "dates.dateTypeId", "Date type")
      );
  }

  private static Map<String, Object> getMap(Instance obj) {
    return new JsonObject(Json.encode(obj)).getMap();
  }
}