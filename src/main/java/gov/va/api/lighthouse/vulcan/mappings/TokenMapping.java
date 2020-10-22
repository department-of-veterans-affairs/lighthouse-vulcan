package gov.va.api.lighthouse.vulcan.mappings;

import gov.va.api.lighthouse.vulcan.CircuitBreaker;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

@Value
@Builder
@Slf4j
public class TokenMapping<EntityT> implements SingleParameterMapping<EntityT> {

  String parameterName;
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
    String fieldName = fieldNameSelector.apply(token);
    Collection<String> values = valueSelector().apply(token);
    if (values.size() == 1) {
      log.info("{} only one value {}", parameterName, values);
      return (root, criteriaQuery, criteriaBuilder) ->
          criteriaBuilder.equal(root.get(fieldName), values.stream().findFirst().orElseThrow());
    }
    log.info("{} multiple values {}", parameterName, values);
    return (root, criteriaQuery, criteriaBuilder) -> {
      In<String> in = criteriaBuilder.in(root.get(fieldName));
      values.forEach(in::value);
      return criteriaBuilder.or(in);
    };
  }
}
