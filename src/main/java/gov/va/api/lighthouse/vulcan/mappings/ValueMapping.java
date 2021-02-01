package gov.va.api.lighthouse.vulcan.mappings;

import gov.va.api.lighthouse.vulcan.CircuitBreaker;
import gov.va.api.lighthouse.vulcan.Predicates;
import java.util.Map;
import java.util.function.Function;
import javax.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.ToString.Exclude;
import lombok.Value;
import org.springframework.data.jpa.domain.Specification;

/**
 * Provides value equality mapping. Unlike StringMapping, this mapping only provides strict
 * equality. However, it does provide a value conversion function that supports multiple columns.
 * Columns will use AND semantics when searching.
 */
@Value
@Builder
public class ValueMapping<EntityT> implements SingleParameterMapping<EntityT> {

  String parameterName;

  /** Produce a JPA entity field name to query value. */
  @Exclude Function<String, Map<String, ?>> converter;

  /** Simple converter that allows a single field to be mapped to the parameter value. */
  public static Function<String, Map<String, ?>> singleFieldValue(String fieldName) {
    return value -> Map.of(fieldName, value);
  }

  @Override
  public Specification<EntityT> specificationFor(HttpServletRequest request) {
    String parameterValue = request.getParameter(parameterName());
    var fieldsToValue = converter.apply(parameterValue);
    if (fieldsToValue.isEmpty()) {
      throw CircuitBreaker.noResultsWillBeFound(
          parameterName(), parameterValue, "No fields were identified to search");
    }
    return (root, criteriaQuery, criteriaBuilder) ->
        fieldsToValue.entrySet().stream()
            .map(e -> criteriaBuilder.equal(root.get(e.getKey()), e.getValue()))
            .collect(Predicates.andUsing(criteriaBuilder));
  }
}
