package gov.va.api.lighthouse.vulcan.mappings;

import static gov.va.api.lighthouse.vulcan.Specifications.selectInList;
import static java.util.stream.Collectors.toSet;

import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

/**
 * Supports CSV list searching.
 *
 * <ul>
 *   <li>Whitespace is ignored around delimiters.
 *   <li>Values are interpreted as an "in class", e.g., name=x,y,z means: Is the value of the JPA
 *       field in the set of x, y, and z. Said differently, is the value of the JPA field equal to
 *       x, or y, or z.
 * </ul>
 */
@Deprecated
@Value
@Builder
public class CsvListMapping<EntityT> implements SingleParameterMapping<EntityT> {

  String parameterName;

  String fieldName;

  @Override
  public Specification<EntityT> specificationFor(HttpServletRequest request) {
    var values =
        Stream.of(request.getParameter(parameterName()).split("\\s*,\\s*"))
            .filter(StringUtils::isNotBlank)
            .collect(toSet());
    return selectInList(fieldName(), values);
  }
}
