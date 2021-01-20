package gov.va.api.lighthouse.vulcan.mappings;

import gov.va.api.lighthouse.vulcan.CircuitBreaker;
import gov.va.api.lighthouse.vulcan.InvalidRequest;
import gov.va.api.lighthouse.vulcan.Mapping;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

@Value
@ToString(onlyExplicitlyIncluded = true)
@Builder
public class ReferenceMapping<EntityT> implements Mapping<EntityT> {
  @Include String parameterName;

  String defaultResourceType;

  Set<String> allowedReferenceTypes;

  Function<ReferenceParameter, Collection<String>> fieldNameSelector;

  Predicate<ReferenceParameter> isSupported;

  Function<ReferenceParameter, String> valueSelector;

  @Override
  public boolean appliesTo(HttpServletRequest request) {
    return asParameterWithType().anyMatch(StringUtils::isNotBlank);
  }

  private Stream<String> asParameterWithType() {
    return allowedReferenceTypes().stream().map(type -> parameterName() + ":" + type);
  }

  @Override
  public Specification<EntityT> specificationFor(HttpServletRequest request) {
    ReferenceParameter referenceParameter =
        ReferenceParameterParser.builder()
            .parameterName(parameterName)
            .parameterValue(request.getParameter(parameterName))
            .allowedReferenceTypes(allowedReferenceTypes)
            .defaultResourceType(defaultResourceType)
            .build()
            .parse();
    if (!referenceParameter.type().isBlank()
        && !allowedReferenceTypes.contains(referenceParameter.type())) {
      throw InvalidRequest.because(
          String.format(
              "ReferenceParameter type [%s] is not legal as per the spec. Allowed types are: %s",
              referenceParameter.type(), allowedReferenceTypes()));
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
