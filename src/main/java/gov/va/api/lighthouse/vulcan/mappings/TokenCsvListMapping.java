package gov.va.api.lighthouse.vulcan.mappings;

import static java.util.stream.Collectors.toList;

import gov.va.api.lighthouse.vulcan.CircuitBreaker;
import gov.va.api.lighthouse.vulcan.Specifications;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
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
public class TokenCsvListMapping<EntityT> implements SingleParameterMapping<EntityT> {

  @Include String parameterName;
  Predicate<TokenParameter> supportedToken;
  Function<TokenParameter, String> fieldNameSelector;
  Function<TokenParameter, Collection<String>> valueSelector;

  @Override
  public Specification<EntityT> specificationFor(HttpServletRequest request) {

    List<TokenParameter> supportedTokens =
        Stream.of(request.getParameter(parameterName()).split("\\s*,\\s*"))
            .map(value -> TokenParameter.parse(parameterName(), value))
            .filter(supportedToken())
            .collect(toList());

    if (supportedTokens.isEmpty()) {
      throw CircuitBreaker.noResultsWillBeFound(
          parameterName(), request.getParameter(parameterName()), "No tokens are not supported");
    }

    return supportedTokens.stream()
        .map(
            token ->
                Specifications.<EntityT>selectInList(
                    fieldNameSelector().apply(token), valueSelector().apply(token)))
        .collect(Specifications.any());
  }
}
