package org.folio.util;

public class Constants {
  private Constants(){}

  public static final String ITEMS_URL = "/inventory/items";
  public static final String CIRCULATION_ITEM_URL = "/circulation-item";
  public static final String HOLDINGS_URL = "/holdings-storage/holdings";
  public static final String TEMPLATES_URL = "/templates";
  public static final String USERS_URL = "/users";
  public static final String CANCELLATION_REASONS_URL = "/cancellation-reason-storage/cancellation-reasons";
  public static final String URL_WITH_ID_PATTERN = "%s/%s";
  public static final String SYSTEM = "System";
  public static final String UUID_PATTERN = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$";
  public static final String NO_BARCODE = "-";
}
