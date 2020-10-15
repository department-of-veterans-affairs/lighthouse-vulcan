package gov.va.api.lighthouse.vulcan;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Mapping builder for an entity. This provides easy to use methods to creating the different types
 * of common mappings.
 */
public class Mappings<EntityT> implements Supplier<List<Mapping<EntityT>>> {

  private final List<Mapping<EntityT>> mappings = new ArrayList<>();

  /** Create a new Mappings instance. */
  public static <E> Mappings<E> forEntity(@SuppressWarnings("unused") Class<E> entity) {
    return new Mappings<>();
  }

  /** Create a CSV list mapping where request and field name are the same. */
  public Mappings<EntityT> csvList(String parameterAndFieldName) {
    mappings.add(
        CsvListMapping.<EntityT>builder()
            .parameterName(parameterAndFieldName)
            .fieldName(parameterAndFieldName)
            .build());
    return this;
  }

  /** Create a CSV list mapping where request and field name are different. */
  public Mappings<EntityT> csvList(String parameterName, String fieldName) {
    mappings.add(
        CsvListMapping.<EntityT>builder()
            .parameterName(parameterName)
            .fieldName(fieldName)
            .build());
    return this;
  }

  /** Get the mappings. */
  @Override
  public List<Mapping<EntityT>> get() {
    return mappings;
  }

  /** Create an instant mapping where request and field name are the same. */
  public Mappings<EntityT> instant(String parameterAndFieldName) {
    mappings.add(
        InstantMapping.<EntityT>builder()
            .parameterName(parameterAndFieldName)
            .fieldName(parameterAndFieldName)
            .build());
    return this;
  }

  /** Create a instant mapping where request and field name are different. */
  public Mappings<EntityT> instant(String parameterName, String fieldName) {
    mappings.add(
        InstantMapping.<EntityT>builder()
            .parameterName(parameterName)
            .fieldName(fieldName)
            .build());
    return this;
  }

  /** Create a string mapping where request and field name are the same. */
  public Mappings<EntityT> string(String parameterAndFieldName) {
    mappings.add(
        StringMapping.<EntityT>builder()
            .parameterName(parameterAndFieldName)
            .fieldName(parameterAndFieldName)
            .build());
    return this;
  }

  /** Create a string mapping where request and field name are different. */
  public Mappings<EntityT> string(String parameterName, String fieldName) {
    mappings.add(
        StringMapping.<EntityT>builder().parameterName(parameterName).fieldName(fieldName).build());
    return this;
  }

  /** Create a value mapping where request and field name are the same. */
  public Mappings<EntityT> value(String parameterAndFieldName, Function<String, ?> converter) {
    mappings.add(
        ValueMapping.<EntityT>builder()
            .parameterName(parameterAndFieldName)
            .fieldName(parameterAndFieldName)
            .converter(converter)
            .build());
    return this;
  }

  /** Create a value mapping where request and field name are different. */
  public Mappings<EntityT> value(
      String parameterName, String fieldName, Function<String, ?> converter) {
    mappings.add(
        ValueMapping.<EntityT>builder()
            .parameterName(parameterName)
            .fieldName(fieldName)
            .converter(converter)
            .build());
    return this;
  }
}
