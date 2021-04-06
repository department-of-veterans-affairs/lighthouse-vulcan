package gov.va.api.lighthouse.vulcan.mappings;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import gov.va.api.lighthouse.vulcan.Mapping;
import gov.va.api.lighthouse.vulcan.mappings.DateMapping.PredicateFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Mapping builder for an entity. This provides easy to use methods to creating the different types
 * of common mappings.
 */
@SuppressWarnings("unused")
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

  /** Create a reference mapping where field name is constant. */
  public Mappings<EntityT> reference(
      String parameterName,
      String fieldName,
      Set<String> allowedResourceTypes,
      String defaultResourceType,
      Predicate<ReferenceParameter> supportedReference,
      Function<ReferenceParameter, String> valueSelector) {
    return reference(
        parameterName,
        t -> singletonList(fieldName),
        allowedResourceTypes,
        defaultResourceType,
        supportedReference,
        valueSelector);
  }

  /** Create a reference mapping where parameterName and fieldName are equal. */
  public Mappings<EntityT> reference(
      String parameterAndFieldName,
      Set<String> allowedResourceTypes,
      String defaultResourceType,
      Predicate<ReferenceParameter> supportedReference,
      Function<ReferenceParameter, String> valueSelector) {
    return reference(
        parameterAndFieldName,
        parameterAndFieldName,
        allowedResourceTypes,
        defaultResourceType,
        supportedReference,
        valueSelector);
  }

  /** Create a reference mapping that is totally configurable. */
  public Mappings<EntityT> reference(
      String parameterName,
      Function<ReferenceParameter, Collection<String>> fieldNameSelector,
      Set<String> allowedResourceTypes,
      String defaultResourceType,
      Predicate<ReferenceParameter> supportedReference,
      Function<ReferenceParameter, String> valueSelector) {
    return add(
        ReferenceMapping.<EntityT>builder()
            .parameterName(parameterName)
            .fieldNameSelector(fieldNameSelector)
            .defaultResourceType(defaultResourceType)
            .allowedReferenceTypes(allowedResourceTypes)
            .supportedReference(supportedReference)
            .valueSelector(valueSelector)
            .build());
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

  /** Create a token list mapping where field name is constant . */
  public Mappings<EntityT> token(
      String parameterName,
      String fieldName,
      Predicate<TokenParameter> supportedToken,
      Function<TokenParameter, Collection<String>> valueSelector) {
    return token(parameterName, t -> singletonList(fieldName), supportedToken, valueSelector);
  }

  /** Create a token list mapping where parameter and field name are the same. */
  public Mappings<EntityT> token(
      String parameterAndFieldName,
      Predicate<TokenParameter> supportedToken,
      Function<TokenParameter, Collection<String>> valueSelector) {
    return token(parameterAndFieldName, parameterAndFieldName, supportedToken, valueSelector);
  }

  /** Create a token mapping where all aspects are configurable. */
  public Mappings<EntityT> token(
      String parameterName,
      Function<TokenParameter, Collection<String>> fieldNameSelector,
      Predicate<TokenParameter> supportedToken,
      Function<TokenParameter, Collection<String>> valueSelector) {
    return tokens(
        parameterName,
        supportedToken,
        t ->
            SelectorSpecificationCollector.<EntityT>builder()
                .orSelectors(
                    fieldNameSelector.apply(t).stream()
                        .map(fieldName -> Selector.<EntityT>of(fieldName, valueSelector.apply(t)))
                        .collect(toList()))
                .build());
  }

  /** Create a token list mapping where field name is constant . */
  public Mappings<EntityT> tokenList(
      String parameterName,
      String fieldName,
      Predicate<TokenParameter> supportedToken,
      Function<TokenParameter, Collection<String>> valueSelector) {
    return tokenList(parameterName, t -> singletonList(fieldName), supportedToken, valueSelector);
  }

  /** Create a token list mapping where parameter and field name are the same. */
  public Mappings<EntityT> tokenList(
      String parameterAndFieldName,
      Predicate<TokenParameter> supportedToken,
      Function<TokenParameter, Collection<String>> valueSelector) {
    return tokenList(parameterAndFieldName, parameterAndFieldName, supportedToken, valueSelector);
  }

  /** Create a token list mapping where all aspects are configurable. */
  public Mappings<EntityT> tokenList(
      String parameterName,
      Function<TokenParameter, Collection<String>> fieldNameSelector,
      Predicate<TokenParameter> supportedToken,
      Function<TokenParameter, Collection<String>> valueSelector) {
    return add(
        TokenCsvListMapping.<EntityT>builder()
            .parameterName(parameterName)
            .supportedToken(supportedToken)
            .fieldNameSelector(fieldNameSelector)
            .valueSelector(valueSelector)
            .build());
  }

  /** Create a token mapping where all aspects are configurable. */
  public Mappings<EntityT> tokens(
      String parameterName,
      Predicate<TokenParameter> supportedToken,
      Function<TokenParameter, SelectorSpecificationCollector<EntityT>> selector) {
    return add(
        TokenMapping.<EntityT>builder()
            .parameterName(parameterName)
            .supportedToken(supportedToken)
            .whereClauseSelector(selector)
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
    return values(parameterName, v -> Map.of(fieldName, converter.apply(v)));
  }

  public Mappings<EntityT> values(
      String parameterName, Function<String, Map<String, ?>> converter) {
    return add(
        ValueMapping.<EntityT>builder().parameterName(parameterName).converter(converter).build());
  }
}
