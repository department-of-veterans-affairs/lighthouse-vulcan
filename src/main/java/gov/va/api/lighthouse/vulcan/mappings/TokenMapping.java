package gov.va.api.lighthouse.vulcan.mappings;

import static gov.va.api.lighthouse.vulcan.Specifications.selectInList;

import gov.va.api.lighthouse.vulcan.CircuitBreaker;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.ToString;
import lombok.ToString.Include;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

@Value
@ToString(onlyExplicitlyIncluded = true)
@Builder
@Slf4j
public class TokenMapping<EntityT> implements SingleParameterMapping<EntityT> {

  @Include String parameterName;
  Predicate<TokenParameter> supportedToken;
  Function<TokenParameter, String> fieldNameSelector;
  Function<TokenParameter, Collection<String>> valueSelector;

  @Override
  public Specification<EntityT> specificationFor(HttpServletRequest request) {
    TokenParameter token =
        TokenParameter.parse(parameterName(), request.getParameter(parameterName()));
    if (!supportedToken.test(token)) {
      log.info("{} token is not supported: {}", parameterName, token);
      throw CircuitBreaker.noResultsWillBeFound(
          parameterName, request.getParameter(parameterName()), "Token is not supported");
    }
    return selectInList(fieldNameSelector.apply(token), valueSelector.apply(token));
  }
}
