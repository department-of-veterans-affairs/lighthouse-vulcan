package gov.va.api.lighthouse.vulcan.mappings;

import static java.util.Collections.singletonList;

import gov.va.api.lighthouse.vulcan.Mapping;
import gov.va.api.lighthouse.vulcan.Specifications;
import gov.va.api.lighthouse.vulcan.mappings.DateMapping.PredicateFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.springframework.data.jpa.domain.Specification;

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

  /**
   * Create a CSV list mapping where request and field name are the same.
   *
   * @deprecated composite value (CSV) support has been added to value mappings. Use one of the
   *     value(...) methods instead.
   */
  @Deprecated(since = "1.0.7", forRemoval = true)
  public Mappings<EntityT> csvList(String parameterAndFieldName) {
    return value(parameterAndFieldName, parameterAndFieldName);
  }

  /**
   * Create a CSV list mapping where request and field name are different.
   *
   * @deprecated composite value (CSV) support has been added to value mappings. Use one of the
   *     value(...) methods instead.
   */
  @Deprecated(since = "1.0.7", forRemoval = true)
  public Mappings<EntityT> csvList(String parameterName, String fieldName) {
    return value(parameterName, fieldName);
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

  /**
   * Add a date mapping for Long entity field values using standard "ap" (approximate) date
   * processing.
   */
  public Mappings<EntityT> dateAsLongMilliseconds(String parameterName, String fieldName) {
    return date(
        parameterName,
        fieldName,
        new DateMapping.LongPredicateFactory(DateMapping.defaultGraduatedApproximation()));
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

  /**
   * Create a token list mapping where field name is constant .
   *
   * @deprecated to support complex token logic, JPA specifications support was added. Use the
   *     tokens(...) method instead.
   */
  @Deprecated(since = "1.0.6", forRemoval = true)
  public Mappings<EntityT> token(
      String parameterName,
      String fieldName,
      Predicate<TokenParameter> supportedToken,
      Function<TokenParameter, Collection<String>> valueSelector) {
    return token(parameterName, t -> singletonList(fieldName), supportedToken, valueSelector);
  }

  /**
   * Create a token list mapping where parameter and field name are the same.
   *
   * @deprecated to support complex token logic, JPA specifications support was added. Use the
   *     tokens(...) method instead.
   */
  @Deprecated(since = "1.0.6", forRemoval = true)
  public Mappings<EntityT> token(
      String parameterAndFieldName,
      Predicate<TokenParameter> supportedToken,
      Function<TokenParameter, Collection<String>> valueSelector) {
    return token(parameterAndFieldName, parameterAndFieldName, supportedToken, valueSelector);
  }

  /**
   * Create a token mapping where all aspects are configurable.
   *
   * @deprecated to support complex token logic, JPA specifications support was added. Use the
   *     tokens(...) method instead.
   */
  @Deprecated(since = "1.0.6", forRemoval = true)
  public Mappings<EntityT> token(
      String parameterName,
      Function<TokenParameter, Collection<String>> fieldNameSelector,
      Predicate<TokenParameter> supportedToken,
      Function<TokenParameter, Collection<String>> valueSelector) {
    return tokens(
        parameterName,
        supportedToken,
        token -> {
          Collection<String> fieldNames = fieldNameSelector.apply(token);
          Collection<String> values = valueSelector.apply(token);
          return fieldNames.stream()
              .map(field -> Specifications.<EntityT>selectInList(field, values))
              .collect(Specifications.any());
        });
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
      Function<TokenParameter, Specification<EntityT>> toSpecification) {
    return add(
        TokenMapping.<EntityT>builder()
            .parameterName(parameterName)
            .supportedToken(supportedToken)
            .toSpecification(toSpecification)
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
