package gov.va.api.lighthouse.vulcan;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

/**
 * The RequestContext provides additional details about the request beyond the HttpServletRequest.
 * This contains configuration data (which influences behavior while processing the request), and
 * information derived from the request.
 */
@Value
@Slf4j
public class RequestContext<EntityT> {

  VulcanConfiguration<EntityT> config;
  HttpServletRequest request;
  Specification<EntityT> specification;
  int page;
  int count;
  PageRequest pageRequest;
  boolean abortSearch;

  @Builder
  private RequestContext(
      @NonNull VulcanConfiguration<EntityT> config, @NonNull HttpServletRequest request) {
    this.config = config;
    this.request = request;
    page = pageValueOf(request);
    count = countValueOf(request);
    pageRequest = PageRequest.of(page - 1, Math.max(count, 1), config.paging().sort());
    checkRules();
    Specification<EntityT> maybeSpecification;
    try {
      maybeSpecification = specificationOf(request);
    } catch (CircuitBreaker e) {
      maybeSpecification = null;
      log.info("Circuit breaker thrown, skipping search: {}", e.getMessage());
    }
    specification = maybeSpecification;
    abortSearch = (specification == null);
  }

  public static <E> RequestContextBuilder<E> forConfig(VulcanConfiguration<E> configuration) {
    return RequestContext.<E>builder().config(configuration);
  }

  private void checkRules() {
    config
        .rules()
        .forEach(
            r -> {
              try {
                r.check(request);
              } catch (InvalidRequest e) {
                log.info("Rejecting request: {}", e.getMessage());
                throw e;
              }
            });
  }

  public boolean countOnly() {
    return count == 0;
  }

  /**
   * Determine a usable paging count value from the request. This will return the default count if
   * the request does not include any count parameters. It will thrown an InvalidParameter exception
   * if the count is not a number or out of range.
   */
  private int countValueOf(HttpServletRequest request) {
    String value = request.getParameter(config.paging().countParameter());
    if (isBlank(value)) {
      return config.paging().defaultCount();
    }
    try {
      int count = Integer.parseInt(value);
      if (count < 0 || count > config.paging().maxCount()) {
        throw invalidCountParameter(value);
      }
      return count;
    } catch (NumberFormatException e) {
      throw invalidCountParameter(value);
    }
  }

  private InvalidRequest invalidCountParameter(String value) {
    return InvalidRequest.badParameter(
        config.paging().countParameter(),
        value,
        "Expected number between 0 and " + config.paging().maxCount());
  }

  private InvalidRequest invalidPageParameter(String value) {
    return InvalidRequest.badParameter(
        config.paging().pageParameter(), value, "Expected number greater than or equal to 1");
  }

  /** Return true if the given parameter is either the page or count parameter. */
  public boolean isPagingRelatedParameter(String param) {
    return config().paging().pageParameter().equals(param)
        || config().paging().countParameter().equals(param);
  }

  /**
   * Determine a usable paging page value from the request. This will return the default page if the
   * request does not include any page parameters. It will thrown an InvalidParameter exception if
   * the page is not a number or out of range.
   */
  private int pageValueOf(HttpServletRequest request) {
    String value = request.getParameter(config.paging().pageParameter());
    if (isBlank(value)) {
      return 1;
    }
    try {
      int page = Integer.parseInt(value);
      if (page < 1) {
        throw invalidPageParameter(value);
      }
      return page;
    } catch (NumberFormatException e) {
      throw invalidPageParameter(value);
    }
  }

  private Specification<EntityT> specificationOf(HttpServletRequest request) {
    Specification<EntityT> all =
        config.mappings().stream()
            .filter(m -> m.appliesTo(request))
            .peek(m -> log.info("Applying {}", m))
            .map(m -> m.specificationFor(request))
            .filter(Objects::nonNull)
            .collect(Specifications.all());
    return all == null ? config.defaultQuery().apply(request) : all;
  }
}
