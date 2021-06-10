package gov.va.api.lighthouse.vulcan.mappings;

import static java.util.Collections.singletonList;

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
    return string(parameterName, s -> singletonList(fieldName));
  }

  /** Create a string mapping where the value should match one of many field names. */
  public Mappings<EntityT> string(
      String parameterName, Function<String, Collection<String>> fieldNameSelector) {
    return add(
        StringMapping.<EntityT>builder()
            .parameterName(parameterName)
            .fieldNameSelector(fieldNameSelector)
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
