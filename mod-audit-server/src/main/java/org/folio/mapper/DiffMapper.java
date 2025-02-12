package org.folio.mapper;

import org.folio.domain.diff.ChangeRecordDto;
import org.folio.rest.jaxrs.model.Diff;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DiffMapper {

    @Mapping(target = "fieldChanges", source = "fieldChanges")
    @Mapping(target = "collectionChanges", source = "collectionChanges")
    Diff map(ChangeRecordDto changeRecordDto);
}
