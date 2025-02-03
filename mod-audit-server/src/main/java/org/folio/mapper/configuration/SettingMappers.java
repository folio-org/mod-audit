package org.folio.mapper.configuration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Getter
@Component
@RequiredArgsConstructor
public class SettingMappers {

  private final SettingGroupMapper settingGroupMapper;
  private final SettingMapper settingMapper;
  private final SettingEntityMapper settingEntityMapper;

}
