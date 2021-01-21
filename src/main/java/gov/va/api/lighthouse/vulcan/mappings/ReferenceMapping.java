package gov.va.api.lighthouse.vulcan.mappings;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
import org.springframework.data.jpa.domain.Specification;

@Value
@ToString(onlyExplicitlyIncluded = true)
@Builder
public class ReferenceMapping<EntityT> implements Mapping<EntityT> {
  @Include String parameterName;

  String defaultResourceType;

  Set<String> allowedReferenceTypes;

  Function<ReferenceParameter, Collection<String>> fieldNameSelector;

  Predicate<ReferenceParameter> supportedReference;

  Function<ReferenceParameter, String> valueSelector;

  @Override
  public boolean appliesTo(HttpServletRequest request) {
    return isNotBlank(request.getParameter(parameterName()))
        || asParametersWithTypeModifier().anyMatch(p -> isNotBlank(request.getParameter(p)));
  }

  private Stream<String> asParametersWithTypeModifier() {
    return allowedReferenceTypes().stream().map(type -> parameterName() + ":" + type);
  }

  @Override
  public Specification<EntityT> specificationFor(HttpServletRequest request) {
    String parameterName = null;
    String parameterValue = null;
    for (String n : supportedParameterNames()) {
      parameterValue = request.getParameter(n);
      if (isNotBlank(parameterValue)) {
        parameterName = n;
        break;
      }
    }
    if (isBlank(parameterValue) || isBlank(parameterName)) {
      throw InvalidRequest.noParametersSpecified();
    }

    ReferenceParameter referenceParameter =
        ReferenceParameterParser.builder()
            .parameterName(parameterName)
            .parameterValue(parameterValue)
            .allowedReferenceTypes(allowedReferenceTypes())
            .defaultResourceType(defaultResourceType())
            .formats(ReferenceParameterParser.standardFormatsForResource(defaultResourceType()))
            .build()
            .parse();

    if (!supportedReference().test(referenceParameter)) {
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
    return Stream.concat(Stream.of(parameterName()), asParametersWithTypeModifier())
        .collect(Collectors.toList());
  }
}
