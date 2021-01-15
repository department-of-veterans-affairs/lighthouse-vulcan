package gov.va.api.lighthouse.vulcan.mappings;

import gov.va.api.lighthouse.vulcan.CircuitBreaker;
import gov.va.api.lighthouse.vulcan.InvalidRequest;
import gov.va.api.lighthouse.vulcan.Specifications;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.ToString;
import lombok.ToString.Include;
import lombok.Value;
import org.springframework.data.jpa.domain.Specification;

@Value
@ToString(onlyExplicitlyIncluded = true)
@Builder
public class ReferenceMapping<EntityT> implements SingleParameterMapping<EntityT> {
  @Include String parameterName;

  Set<String> allowedResourceTypes;

  Function<ReferenceParameter, Collection<String>> fieldNameSelector;

  Predicate<ReferenceParameter> isSupported;

  Function<ReferenceParameter, String> valueSelector;

  private Stream<String> asParameterWithType() {
    return allowedResourceTypes().stream().map(type -> parameterName() + ":" + type);
  }

  @Override
  public Specification<EntityT> specificationFor(HttpServletRequest request) {
    ReferenceParameter referenceParameter =
        ReferenceParameter.parse(parameterName(), request.getParameter(parameterName()));
    if (!referenceParameter.type().isBlank()
        && !allowedResourceTypes.contains(referenceParameter.type())) {
      throw InvalidRequest.because(
          String.format(
              "ReferenceParameter type [%s] is not legal as per the spec. Allowed types are: %s",
              referenceParameter.type(), allowedResourceTypes()));
    }
    if (isSupported().test(referenceParameter)) {
      throw CircuitBreaker.noResultsWillBeFound(
          parameterName(), request.getParameter(parameterName()), "Reference is not supported.");
    }
    Collection<String> fieldNames = fieldNameSelector().apply(referenceParameter);
    if (fieldNames.isEmpty()) {
      throw CircuitBreaker.noResultsWillBeFound(
          parameterName(), request.getParameter(parameterName()), "No database column defined.");
    }
    String value = valueSelector().apply(referenceParameter);
    return fieldNames.stream()
        .map(
            field ->
                (Specification<EntityT>)
                    (root, criteriaQuery, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get(field), value))
        .collect(Specifications.any());
  }

  @Override
  public List<String> supportedParameterNames() {
    return Stream.concat(Stream.of(parameterName()), asParameterWithType())
        .collect(Collectors.toList());
  }
}
