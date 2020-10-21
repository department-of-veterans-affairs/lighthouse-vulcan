package gov.va.api.lighthouse.vulcan;

import gov.va.api.lighthouse.vulcan.VulcanResult.Paging;
import java.util.Optional;
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

  private VulcanResult<EntityT> countOnlyResult(RequestContext<EntityT> context) {
    long totalRecords = repository.count(context.specification());
    // TODO PAGING FOR COUNT ONLY
    return VulcanResult.<EntityT>builder()
        .paging(Paging.builder().build())
        .entities(Stream.empty())
        .build();
  }

  /** Process there request and return a non-null list of database entities that apply. */
  public VulcanResult<EntityT> forge(HttpServletRequest request) {
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
                  .firstPage(Optional.empty())
                  .previousPage(Optional.empty())
                  .thisPage(Optional.empty())
                  .nextPage(Optional.empty())
                  .lastPage(Optional.empty())
                  .build())
          .entities(Stream.empty())
          .build();
    }
    if (context.countOnly()) {
      return countOnlyResult(context);
    }
    return pageOfRecords(context);
  }

  private VulcanResult<EntityT> pageOfRecords(RequestContext<EntityT> context) {
    Page<EntityT> searchResult = repository.findAll(context.specification(), context.pageRequest());
    boolean hasPages = searchResult.getTotalElements() > 0;
    int thisPage = context.page();
    Integer firstPage = hasPages ? 1 : null;
    Integer lastPage = hasPages ? searchResult.getTotalPages() : null;
    Integer previousPage = hasPages && thisPage > 1 && thisPage <= lastPage ? (thisPage - 1) : null;
    Integer nextPage = hasPages && thisPage < lastPage ? (thisPage + 1) : null;

    return VulcanResult.<EntityT>builder()
        .paging(
            Paging.builder()
                .totalPages(searchResult.getTotalPages())
                .totalRecords(searchResult.getTotalElements())
                .firstPage(Optional.ofNullable(firstPage))
                .previousPage(Optional.ofNullable(previousPage))
                .thisPage(Optional.ofNullable(thisPage))
                .nextPage(Optional.ofNullable(nextPage))
                .lastPage(Optional.ofNullable(lastPage))
                .build())
        .entities(searchResult.stream())
        .build();
  }

  @Value
  @Builder
  public static class PagingParameters {
    @NonNull String pageParameter;
    @NotNull String countParameter;
    @Builder.Default int defaultCount = 10;
    @Builder.Default int maxCount = 20;
    @NotNull Sort sort;
  }
}
