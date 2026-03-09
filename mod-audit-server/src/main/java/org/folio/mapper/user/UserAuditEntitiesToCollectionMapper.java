package org.folio.mapper.user;

import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.folio.dao.user.UserAuditEntity;
import org.folio.rest.jaxrs.model.UserAuditCollection;
import org.folio.rest.jaxrs.model.UserAuditItem;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAuditEntitiesToCollectionMapper implements Function<List<UserAuditEntity>, UserAuditCollection> {

  private final Function<UserAuditEntity, UserAuditItem> entityToItemMapper;

  @Override
  public UserAuditCollection apply(List<UserAuditEntity> userAuditEntities) {
    var items = userAuditEntities.stream()
      .map(entityToItemMapper)
      .toList();
    return new UserAuditCollection().withUserAuditItems(items);
  }
}
