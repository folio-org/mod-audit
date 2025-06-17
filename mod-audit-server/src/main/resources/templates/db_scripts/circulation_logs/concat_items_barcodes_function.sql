CREATE OR REPLACE FUNCTION ${myuniversity}_${mymodule}.concat_items_barcodes(jsonb_array jsonb) RETURNS text AS $$
  SELECT string_agg(item->>'itemBarcode', ' ')
  FROM jsonb_array_elements($1) as item
  WHERE item->>'itemBarcode' IS NOT NULL;
$$ LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT;