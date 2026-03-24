package org.folio.services.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.folio.domain.diff.ChangeRecordDto;
import org.folio.domain.diff.CollectionChangeDto;
import org.folio.domain.diff.CollectionItemChangeDto;
import org.folio.domain.diff.FieldChangeDto;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class UserFieldExclusionFilterTest {

  // --- parseExcludedFields tests ---

  @Test
  void parseExcludedFields_shouldReturnEmptySet_whenNull() {
    assertThat(UserFieldExclusionFilter.parseExcludedFields(null)).isEmpty();
  }

  @Test
  void parseExcludedFields_shouldReturnEmptySet_whenEmptyString() {
    assertThat(UserFieldExclusionFilter.parseExcludedFields("")).isEmpty();
  }

  @Test
  void parseExcludedFields_shouldReturnEmptySet_whenBlankString() {
    assertThat(UserFieldExclusionFilter.parseExcludedFields("  ")).isEmpty();
  }

  @Test
  void parseExcludedFields_shouldReturnEmptySet_whenEmptyJsonArray() {
    assertThat(UserFieldExclusionFilter.parseExcludedFields("[]")).isEmpty();
  }

  @Test
  void parseExcludedFields_shouldReturnEmptySet_whenMalformedJson() {
    assertThat(UserFieldExclusionFilter.parseExcludedFields("not json")).isEmpty();
  }

  @Test
  void parseExcludedFields_shouldReturnEmptySet_whenNotString() {
    assertThat(UserFieldExclusionFilter.parseExcludedFields(42)).isEmpty();
  }

  @Test
  void parseExcludedFields_shouldParseValidJsonArray() {
    var result = UserFieldExclusionFilter.parseExcludedFields("[\"personal.email\",\"barcode\"]");
    assertThat(result).containsExactlyInAnyOrder("personal.email", "barcode");
  }

  // --- applyExclusion tests ---

  @Test
  void applyExclusion_shouldReturnNull_whenDiffIsNull() {
    assertThat(UserFieldExclusionFilter.applyExclusion(null, Set.of("barcode"))).isNull();
  }

  @Test
  void applyExclusion_shouldReturnDiffUnchanged_whenExcludedPathsEmpty() {
    var diff = new ChangeRecordDto(
      List.of(FieldChangeDto.modified("username", "username", "old", "new")), null);
    assertThat(UserFieldExclusionFilter.applyExclusion(diff, Set.of())).isSameAs(diff);
  }

  @Test
  void applyExclusion_shouldFilterFieldChangeByExactPath() {
    var diff = new ChangeRecordDto(List.of(
      FieldChangeDto.modified("username", "username", "old", "new"),
      FieldChangeDto.modified("barcode", "barcode", "old", "new")
    ), null);

    var result = UserFieldExclusionFilter.applyExclusion(diff, Set.of("barcode"));

    assertThat(result).isNotNull();
    assertThat(result.getFieldChanges()).hasSize(1);
    assertThat(result.getFieldChanges().get(0).getFullPath()).isEqualTo("username");
  }

  @Test
  void applyExclusion_shouldFilterNestedFieldChange() {
    var diff = new ChangeRecordDto(List.of(
      FieldChangeDto.modified("email", "personal.email", "old@test.com", "new@test.com"),
      FieldChangeDto.modified("username", "username", "old", "new")
    ), null);

    var result = UserFieldExclusionFilter.applyExclusion(diff, Set.of("personal.email"));

    assertThat(result).isNotNull();
    assertThat(result.getFieldChanges()).hasSize(1);
    assertThat(result.getFieldChanges().get(0).getFullPath()).isEqualTo("username");
  }

  @Test
  void applyExclusion_shouldFilterByParentPath() {
    var diff = new ChangeRecordDto(List.of(
      FieldChangeDto.modified("myField", "customFields.myField", "old", "new"),
      FieldChangeDto.modified("otherField", "customFields.otherField", "old", "new"),
      FieldChangeDto.modified("username", "username", "old", "new")
    ), null);

    var result = UserFieldExclusionFilter.applyExclusion(diff, Set.of("customFields"));

    assertThat(result).isNotNull();
    assertThat(result.getFieldChanges()).hasSize(1);
    assertThat(result.getFieldChanges().get(0).getFullPath()).isEqualTo("username");
  }

  @Test
  void applyExclusion_shouldFilterCollectionChangeByExactFullPath() {
    var diff = new ChangeRecordDto(
      List.of(FieldChangeDto.modified("username", "username", "old", "new")),
      List.of(new CollectionChangeDto("departments", "departments",
        List.of(CollectionItemChangeDto.added("dept1"))))
    );

    var result = UserFieldExclusionFilter.applyExclusion(diff, Set.of("departments"));

    assertThat(result).isNotNull();
    assertThat(result.getFieldChanges()).hasSize(1);
    assertThat(result.getCollectionChanges()).isEmpty();
  }

  @Test
  void applyExclusion_shouldExcludeCollectionChangeByParentPathPrefix() {
    var diff = new ChangeRecordDto(
      List.of(FieldChangeDto.modified("username", "username", "old", "new")),
      List.of(new CollectionChangeDto("tags.tagList", "tagList",
        List.of(CollectionItemChangeDto.added("tag1"))))
    );

    var result = UserFieldExclusionFilter.applyExclusion(diff, Set.of("tags"));

    assertThat(result).isNotNull();
    assertThat(result.getFieldChanges()).hasSize(1);
    assertThat(result.getCollectionChanges()).isEmpty();
  }

  @Test
  void applyExclusion_shouldFilterPersonalAddresses() {
    var diff = new ChangeRecordDto(null,
      List.of(new CollectionChangeDto("personal.addresses", "addresses",
        List.of(CollectionItemChangeDto.added("addr1"))))
    );

    var result = UserFieldExclusionFilter.applyExclusion(diff, Set.of("personal.addresses"));

    assertThat(result).isNull();
  }

  @Test
  void applyExclusion_shouldReturnNull_whenAllChangesExcluded() {
    var diff = new ChangeRecordDto(
      List.of(FieldChangeDto.modified("barcode", "barcode", "old", "new")),
      List.of(new CollectionChangeDto("departments", "departments",
        List.of(CollectionItemChangeDto.added("dept1"))))
    );

    var result = UserFieldExclusionFilter.applyExclusion(diff, Set.of("barcode", "departments"));

    assertThat(result).isNull();
  }

  @Test
  void applyExclusion_shouldFilterSomeFieldsAndSomeCollections() {
    var diff = new ChangeRecordDto(
      List.of(
        FieldChangeDto.modified("username", "username", "old", "new"),
        FieldChangeDto.modified("barcode", "barcode", "old", "new"),
        FieldChangeDto.modified("email", "personal.email", "old@test.com", "new@test.com")
      ),
      List.of(
        new CollectionChangeDto("departments", "departments",
          List.of(CollectionItemChangeDto.added("dept1"))),
        new CollectionChangeDto("personal.addresses", "addresses",
          List.of(CollectionItemChangeDto.added("addr1")))
      )
    );

    var result = UserFieldExclusionFilter.applyExclusion(diff, Set.of("barcode", "personal"));

    assertThat(result).isNotNull();
    assertThat(result.getFieldChanges()).hasSize(1);
    assertThat(result.getFieldChanges().get(0).getFullPath()).isEqualTo("username");
    assertThat(result.getCollectionChanges()).hasSize(1);
    assertThat(result.getCollectionChanges().get(0).getFullPath()).isEqualTo("departments");
  }

  @Test
  void applyExclusion_shouldNotExcludePartialPrefixMatch() {
    var diff = new ChangeRecordDto(List.of(
      FieldChangeDto.modified("barcodeExtra", "barcodeExtra", "old", "new")
    ), null);

    var result = UserFieldExclusionFilter.applyExclusion(diff, Set.of("barcode"));

    assertThat(result).isNotNull();
    assertThat(result.getFieldChanges()).hasSize(1);
    assertThat(result.getFieldChanges().get(0).getFullPath()).isEqualTo("barcodeExtra");
  }
}
