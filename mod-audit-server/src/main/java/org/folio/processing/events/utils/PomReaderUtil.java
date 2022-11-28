package org.folio.processing.events.utils;

public enum PomReaderUtil {
  INSTANCE;

  public String constructModuleVersionAndVersion(String moduleName, String moduleVersion) {
    String result = moduleName.replace("_", "-");
    return result + "-" + moduleVersion;
  }

}
