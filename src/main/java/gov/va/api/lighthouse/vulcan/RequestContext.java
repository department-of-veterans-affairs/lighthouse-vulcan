package gov.va.api.lighthouse.vulcan;

import static org.apache.commons.lang3.StringUtils.isBlank;

import javax.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@Value
@Slf4j
public class RequestContext<EntityT> {

  VulcanConfiguration<EntityT> config;

  HttpServletRequest request;

  Specification<EntityT> specification;

  int page;

  int count;

  PageRequest pageRequest;

  @Builder
  private RequestContext(
      @NonNull VulcanConfiguration<EntityT> config, @NonNull HttpServletRequest request) {
    this.config = config;
    this.request = request;
    specification = specificationOf(request);
    page = pageValueOf(request);
    count = countValueOf(request);
    pageRequest = PageRequest.of(page - 1, count, config.paging().sort());
  }

  public static <E> RequestContextBuilder<E> forConfig(VulcanConfiguration<E> configuration) {
    return RequestContext.<E>builder().config(configuration);
  }

  /**
   * Determine a usuable paging count value from the request. This will return the default count if
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

  private InvalidParameter invalidCountParameter(String value) {
    return InvalidParameter.badValue(
        config.paging().countParameter(),
        value,
        "Expected number between 0 and " + config.paging().maxCount());
  }

  /**
   * Determine a usuable paging page value from the request. This will return the default page if
   * the request does not include any page parameters. It will thrown an InvalidParameter exception
   * if the page is not a number or out of range.
   */
  private int pageValueOf(HttpServletRequest request) {
    String value = request.getParameter(config.paging().pageParameter());
    if (isBlank(value)) {
      return 1;
    }
    try {
      int page = Integer.parseInt(value);
      if (page < 1) {
        throw invalidCountParameter(value);
      }
      return page;
    } catch (NumberFormatException e) {
      throw invalidCountParameter(value);
    }
  }

  private Specification<EntityT> specificationOf(HttpServletRequest request) {
    return config.mappings().stream()
        .filter(m -> m.appliesTo(request))
        .peek(
            m -> {
              log.info("Applying {}", m);
            })
        .map(m -> m.specificationFor(request))
        .collect(Specifications.and());
  }
}
