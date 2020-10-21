package gov.va.api.lighthouse.vulcan;

import static java.util.Optional.empty;

import gov.va.api.lighthouse.vulcan.VulcanResult.Paging;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * The request processor. This will accept an HTTP request object and generate a database query
 * based on mappings, then execute it. Any query clauses generated from parameters are combined with
 * using AND semantics.
 */
@Builder
@Slf4j
public class Vulcan<EntityT, JpaRepositoryT extends JpaSpecificationExecutor<EntityT>> {

  @NonNull private final JpaRepositoryT repository;

  @NotNull private final VulcanConfiguration<EntityT> config;

  public static <E, R extends JpaSpecificationExecutor<E>> VulcanBuilder<E, R> forRepo(R repo) {
    return Vulcan.<E, R>builder().repository(repo);
  }

  /** When making paging links, use the request URL from the HttpServletRequest object. */
  @SuppressWarnings("JdkObsolete")
  public static BaseUrlStrategy useRequestUrl() {
    /*
     * The compiler is flagging the interaction with StringBuffer as a warning. Neither
     * HttpServletRequest or the getRequestURL method are deprecated, nor are there alternatives
     * that do not return StringBuffer.
     */
    return r -> r.getRequestURL().toString();
  }

  /** When making paging links, use the given base URL. */
  public static BaseUrlStrategy useUrl(String baseUrl) {
    return r -> baseUrl;
  }

  /** Process there request and return a non-null list of database entities that apply. */
  public VulcanResult<EntityT> forge(HttpServletRequest request) {

    // TODO extensible page link strategy
    // TODO - url from request
    // TODO - url from provided (e.g. configured like dq)

    // TODO parameter rules
    // TODO - at least one of
    // TODO - only one onf
    // TODO - required groups of parameters (e.g. if "a" is specified then must specify "b")

    // TODO configurable behavior if no parameters are specified
    // TODO - throw error
    // TODO - select all
    // TODO - return empty (select none)
    // TODO - select with default parameters

    // TODO make paging option so that when executed the JPA Page isn't provided
    // TODO to support Observation query hack

    // TODO prototype usage
    // TODO procedure has superman hack
    // TODO observation has select all hack
    // TODO location/organization has address

    RequestContext<EntityT> context = RequestContext.forConfig(config).request(request).build();

    log.info("specification {}", context.specification());
    if (context.specification() == null) {
      // TODO what to do when no request parameters were specified?
      // TODO Select all
      // TODO Select none
      // TODO Configurable default specification?
      // TODO Error?
      return VulcanResult.<EntityT>builder()
          .paging(
              Paging.builder()
                  .totalRecords(0)
                  .totalPages(0)
                  .firstPage(empty())
                  .firstPageUrl(empty())
                  .previousPage(empty())
                  .previousPageUrl(empty())
                  .thisPage(empty())
                  .thisPageUrl(empty())
                  .nextPage(empty())
                  .nextPageUrl(empty())
                  .lastPage(empty())
                  .lastPageUrl(empty())
                  .build())
          .entities(Stream.empty())
          .build();
    }
    if (context.countOnly()) {
      return resultsForCountOnly(context);
    }
    return resultsForPageOfRecords(context);
  }

  private VulcanResult<EntityT> resultsForCountOnly(RequestContext<EntityT> context) {
    long totalRecords = repository.count(context.specification());
    return VulcanResult.<EntityT>builder()
        .paging(
            Paging.builder()
                .totalRecords(totalRecords)
                .totalPages(0)
                .firstPage(empty())
                .firstPageUrl(empty())
                .previousPage(empty())
                .previousPageUrl(empty())
                .thisPage(Optional.of(context.page()))
                .thisPageUrl(PageLinkBuilder.of(context).urlForPage(context.page()))
                .nextPage(empty())
                .nextPageUrl(empty())
                .lastPage(empty())
                .lastPageUrl(empty())
                .build())
        .entities(Stream.empty())
        .build();
  }

  private VulcanResult<EntityT> resultsForPageOfRecords(RequestContext<EntityT> context) {
    Page<EntityT> searchResult = repository.findAll(context.specification(), context.pageRequest());
    boolean hasPages = searchResult.getTotalElements() > 0;
    int thisPage = context.page();
    Integer firstPage = hasPages ? 1 : null;
    Integer lastPage = hasPages ? searchResult.getTotalPages() : null;
    Integer previousPage = hasPages && thisPage > 1 && thisPage <= lastPage ? (thisPage - 1) : null;
    Integer nextPage = hasPages && thisPage < lastPage ? (thisPage + 1) : null;

    PageLinkBuilder links = PageLinkBuilder.of(context);

    return VulcanResult.<EntityT>builder()
        .paging(
            Paging.builder()
                .totalPages(searchResult.getTotalPages())
                .totalRecords(searchResult.getTotalElements())
                .firstPage(Optional.ofNullable(firstPage))
                .firstPageUrl(links.urlForPage(firstPage))
                .previousPage(Optional.ofNullable(previousPage))
                .previousPageUrl(links.urlForPage(previousPage))
                .thisPage(Optional.of(thisPage))
                .thisPageUrl(links.urlForPage(thisPage))
                .nextPage(Optional.ofNullable(nextPage))
                .nextPageUrl(links.urlForPage(nextPage))
                .lastPage(Optional.ofNullable(lastPage))
                .lastPageUrl(links.urlForPage(lastPage))
                .build())
        .entities(searchResult.stream())
        .build();
  }

  public interface BaseUrlStrategy extends Function<HttpServletRequest, String> {}

  @Value
  @Builder
  public static class PagingParameters {
    @NonNull String pageParameter;
    @NotNull String countParameter;
    @Builder.Default int defaultCount = 10;
    @Builder.Default int maxCount = 20;
    @NotNull Sort sort;
    @NonNull BaseUrlStrategy baseUrlStrategy;
  }
}
