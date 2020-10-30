package gov.va.api.lighthouse.vulcan.mappings;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import gov.va.api.lighthouse.vulcan.Mapping;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.jpa.domain.Specification;

/**
 * Provides parameter mapping for FHIR string type searches. See
 * http://hl7.org/fhir/R4/search.html#string
 *
 * <p>Supports name=xxx for case insensitive "starts with" behavior.
 *
 * <p>Supports name:contains=xxx for case insensitive "contains behavior.
 *
 * <p>Supports name:exact=xxx for case sensitive equality behavior.
 */
@Value
@Builder
public class StringMapping<EntityT> implements Mapping<EntityT> {

  String parameterName;

  String fieldName;

  @Override
  public boolean appliesTo(HttpServletRequest request) {
    return isNotBlank(request.getParameter(asStartsWithParameterName()))
        || isNotBlank(request.getParameter(asContainsParameterName()))
        || isNotBlank(request.getParameter(asExactParameterName()));
  }

  private String asContainsParameterName() {
    return parameterName + ":contains";
  }

  private String asExactParameterName() {
    return parameterName + ":exact";
  }

  private String asStartsWithParameterName() {
    return parameterName;
  }

  private Specification<EntityT> clauseForContainsMatch(HttpServletRequest request) {
    String value = request.getParameter(asContainsParameterName());
    if (isBlank(value)) {
      return null;
    }
    return (Specification<EntityT>)
        (root, criteriaQuery, criteriaBuilder) ->
            criteriaBuilder.like(
                criteriaBuilder.lower(root.get(fieldName)),
                "%" + value.toLowerCase(Locale.ENGLISH) + "%");
  }

  private Specification<EntityT> clauseForExactMatch(HttpServletRequest request) {
    String value = request.getParameter(asExactParameterName());
    return (Specification<EntityT>)
        (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get(fieldName), value);
  }

  private Specification<EntityT> clauseForStartsWithMatch(HttpServletRequest request) {
    String value = request.getParameter(asStartsWithParameterName());
    if (isBlank(value)) {
      return null;
    }
    return (Specification<EntityT>)
        (root, criteriaQuery, criteriaBuilder) ->
            criteriaBuilder.like(
                criteriaBuilder.lower(root.get(fieldName)),
                value.toLowerCase(Locale.ENGLISH) + "%");
  }

  @Override
  public Specification<EntityT> specificationFor(HttpServletRequest request) {
    Specification<EntityT> specification = clauseForStartsWithMatch(request);
    if (specification == null) {
      specification = clauseForContainsMatch(request);
    }
    if (specification == null) {
      specification = clauseForExactMatch(request);
    }
    if (specification == null) {
      throw new IllegalStateException("query parameters do not match any clause type");
    }
    return specification;
  }

  @Override
  public List<String> supportedParameterNames() {
    return List.of(asExactParameterName(), asContainsParameterName(), asStartsWithParameterName());
  }
}
