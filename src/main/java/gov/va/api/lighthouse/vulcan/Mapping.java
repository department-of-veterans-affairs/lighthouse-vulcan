package gov.va.api.lighthouse.vulcan;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.data.jpa.domain.Specification;

/**
 * Generates a JPA specification for searching based on an HTTP request. There is no limitation on
 * what request information implementations may use. Implementations may look at request parameters,
 * headers, body, etc.
 *
 * <p>The Vulcan framework will execute appliesTo to see if the mapping wishes to participate in the
 * request. If true, then specificationFor will be invoked.
 *
 * @param <EntityT> The database entity this mapping will apply to.
 */
public interface Mapping<EntityT> {

  /**
   * Return true if this mapping should be included in the processing of this request. This method
   * is guaranteed to be invoked BEFORE specificationFor.
   */
  boolean appliesTo(HttpServletRequest request);

  /**
   * If appliesTo returns true, this method will be invoked to produce a Specification instance.
   * This method is expected to return a non-null instance that may be as complicated as necessary.
   * Implementations should be aware that the returned Specification may be combined with others
   * using AND or OR semantics. Implementations not assume which.
   */
  Specification<EntityT> specificationFor(HttpServletRequest request);

  /** Return a list of parameter names that are supported by this mapping. */
  List<String> supportedParameterNames();
}
