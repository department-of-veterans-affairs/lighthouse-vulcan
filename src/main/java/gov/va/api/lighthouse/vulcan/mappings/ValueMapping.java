package gov.va.api.lighthouse.vulcan.mappings;

import java.util.function.Function;
import javax.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.jpa.domain.Specification;

/**
 * Provides value equality mapping. Unlike StringMapping, this mapping only provides strict
 * equality. However, it does provide a value conversion function.
 */
@Value
@Builder
public class ValueMapping<EntityT> implements SingleParameterMapping<EntityT> {

  String parameterName;

  String fieldName;

  @Builder.Default Function<String, ?> converter = Function.identity();

  @Override
  public Specification<EntityT> specificationFor(HttpServletRequest request) {
    var value = converter.apply(request.getParameter(parameterName()));
    return (root, criteriaQuery, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(fieldName()), value);
  }
}
