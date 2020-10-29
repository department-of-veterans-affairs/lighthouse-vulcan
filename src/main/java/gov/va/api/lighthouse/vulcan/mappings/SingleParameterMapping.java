package gov.va.api.lighthouse.vulcan.mappings;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import gov.va.api.lighthouse.vulcan.Mapping;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

/**
 * This support interface provides a default implementation that applies to requests where the
 * parameter is specified as a non-blank value.
 */
public interface SingleParameterMapping<EntityT> extends Mapping<EntityT> {

  /** Return true if the parameter is specified in the request with a non-blank value. */
  @Override
  default boolean appliesTo(HttpServletRequest request) {
    return isNotBlank(request.getParameter(parameterName()));
  }

  /** The HTTP request parameter name. */
  String parameterName();

  @Override
  default List<String> supportedParameterNames() {
    return List.of(parameterName());
  }
}
