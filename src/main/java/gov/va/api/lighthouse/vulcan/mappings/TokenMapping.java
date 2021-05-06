package gov.va.api.lighthouse.vulcan.mappings;

import static java.util.stream.Collectors.toList;

import gov.va.api.lighthouse.vulcan.CircuitBreaker;
import gov.va.api.lighthouse.vulcan.Specifications;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
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
public class TokenMapping<EntityT> implements SingleParameterMapping<EntityT> {

  @Include String parameterName;
  Predicate<TokenParameter> supportedToken;
  Function<TokenParameter, Specification<EntityT>> toSpecification;

  @Override
  public Specification<EntityT> specificationFor(HttpServletRequest request) {
    String parameterValue = request.getParameter(parameterName());
    if (parameterValue == null) {
      throw CircuitBreaker.noResultsWillBeFound(
          parameterName(), "null", "Parameter value is null.");
    }
    List<TokenParameter> tokens =
        Arrays.stream(parameterValue.split(",", -1))
            .map(StringUtils::trimToNull)
            .filter(Objects::nonNull)
            .map(v -> TokenParameter.parse(parameterName(), v))
            .filter(supportedToken())
            .collect(toList());
    if (tokens.isEmpty()) {
      throw CircuitBreaker.noResultsWillBeFound(
          parameterName(), parameterValue, "No supported tokens were found.");
    }
    return tokens.stream().map(toSpecification()).collect(Specifications.any());
  }
}
