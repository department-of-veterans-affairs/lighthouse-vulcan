package gov.va.api.lighthouse.vulcan;

import java.time.Instant;
import javax.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

@Value
@Builder
@Slf4j
public class InstantMapping<EntityT> implements SingleParameterMapping<EntityT> {

  String parameterName;

  String fieldName;

  @Override
  public Specification<EntityT> specificationFor(HttpServletRequest request) {
    var value = Instant.parse(request.getParameter(parameterName()));
    log.info("lt {}", value);
    // TODO complex date processing using DQ's DateTimeParameter
    return (root, criteriaQuery, criteriaBuilder) ->
        criteriaBuilder.lessThan(root.get(fieldName()), value);
  }
}
