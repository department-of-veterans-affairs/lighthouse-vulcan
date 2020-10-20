package gov.va.api.lighthouse.vulcan;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

  /** Process there request and return a non-null list of database entities that apply. */
  public List<EntityT> forge(HttpServletRequest request) {

    RequestContext context = RequestContext.forConfig(config).request(request).build();

    Specification<EntityT> specification = context.specification();

    log.info("specification {}", specification);
    if (specification == null) {
      return List.of();
    }
    return repository.findAll(specification);
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
