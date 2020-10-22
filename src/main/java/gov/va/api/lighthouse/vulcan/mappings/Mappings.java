package gov.va.api.lighthouse.vulcan.mappings;

import gov.va.api.lighthouse.vulcan.Mapping;
import gov.va.api.lighthouse.vulcan.mappings.DateMapping.PredicateFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
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

  /** Add any mapping. */
  public Mappings<EntityT> add(Mapping<EntityT> mapping) {
    mappings.add(mapping);
    return this;
  }

  /** Create a CSV list mapping where request and field name are the same. */
  public Mappings<EntityT> csvList(String parameterAndFieldName) {
    return csvList(parameterAndFieldName, parameterAndFieldName);
  }

  /** Create a CSV list mapping where request and field name are different. */
  public Mappings<EntityT> csvList(String parameterName, String fieldName) {
    return add(
        CsvListMapping.<EntityT>builder()
            .parameterName(parameterName)
            .fieldName(fieldName)
            .build());
  }

  /**
   * Add a date mapping using a custom predicate factory to for precise control on how the DB is
   * queried.
   */
  public <DateT> Mappings<EntityT> date(
      String parameterName, String fieldName, PredicateFactory<DateT> predicateFactory) {
    return add(
        DateMapping.<EntityT, DateT>builder()
            .parameterName(parameterName)
            .fieldName(fieldName)
            .predicates(predicateFactory)
            .build());
  }

  /**
   * Add a date mapping for Instant entity field values using standard "ap" (approximate) date
   * processing.
   */
  public Mappings<EntityT> dateAsInstant(String parameterName, String fieldName) {
    return date(
        parameterName,
        fieldName,
        new DateMapping.InstantPredicateFactory(DateMapping.defaultGraduatedApproximation()));
  }

  /** Get the mappings. */
  @Override
  public List<Mapping<EntityT>> get() {
    return mappings;
  }

  /** Create a string mapping where request and field name are the same. */
  public Mappings<EntityT> string(String parameterAndFieldName) {
    return string(parameterAndFieldName, parameterAndFieldName);
  }

  /** Create a string mapping where request and field name are different. */
  public Mappings<EntityT> string(String parameterName, String fieldName) {
    return add(
        StringMapping.<EntityT>builder().parameterName(parameterName).fieldName(fieldName).build());
  }

  public Mappings<EntityT> token(
      String parameterName,
      String fieldName,
      Predicate<TokenParameter> supportedToken,
      Function<TokenParameter, Collection<String>> valueSelector) {
    return token(parameterName, t -> fieldName, supportedToken, valueSelector);
  }

  public Mappings<EntityT> token(
      String parameterAndFieldName,
      Predicate<TokenParameter> supportedToken,
      Function<TokenParameter, Collection<String>> valueSelector) {
    return token(parameterAndFieldName, parameterAndFieldName, supportedToken, valueSelector);
  }

  public Mappings<EntityT> token(
      String parameterName,
      Function<TokenParameter, String> fieldNameSelector,
      Predicate<TokenParameter> supportedToken,
      Function<TokenParameter, Collection<String>> valueSelector) {
    return add(
        TokenMapping.<EntityT>builder()
            .parameterName(parameterName)
            .supportedToken(supportedToken)
            .fieldNameSelector(fieldNameSelector)
            .valueSelector(valueSelector)
            .build());
  }

  /** Create a value mapping where request and field name are the same. */
  public Mappings<EntityT> value(String parameterAndFieldName, Function<String, ?> converter) {
    return value(parameterAndFieldName, parameterAndFieldName, converter);
  }

  /** Create a value mapping where request and field name are the same with no value conversion. */
  public Mappings<EntityT> value(String parameterAndFieldName) {
    return value(parameterAndFieldName, parameterAndFieldName);
  }

  /**
   * Create a value mapping where request and field name are different but with no value conversion.
   */
  public Mappings<EntityT> value(String parameterName, String fieldName) {
    return value(parameterName, fieldName, Function.identity());
  }

  /** Create a value mapping where request and field name are different. */
  public Mappings<EntityT> value(
      String parameterName, String fieldName, Function<String, ?> converter) {
    return add(
        ValueMapping.<EntityT>builder()
            .parameterName(parameterName)
            .fieldName(fieldName)
            .converter(converter)
            .build());
  }
}
