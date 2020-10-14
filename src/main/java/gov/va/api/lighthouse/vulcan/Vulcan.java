package gov.va.api.lighthouse.vulcan;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
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

  private final JpaRepositoryT repository;
  @Singular private final List<Mapping<EntityT>> mappings;

  public static <E, R extends JpaSpecificationExecutor<E>> VulcanBuilder<E, R> forRepo(R repo) {
    return Vulcan.<E, R>builder().repository(repo);
  }

  /** Process there request and return a non-null list of database entities that apply. */
  public List<EntityT> forge(HttpServletRequest request) {

    Specification<EntityT> specification =
        mappings.stream()
            .filter(m -> m.appliesTo(request))
            .peek(
                m -> {
                  log.info("Applying {}", m);
                })
            .map(m -> m.specificationFor(request))
            .collect(Specifications.and());

    log.info("specification {}", specification);
    if (specification == null) {
      return List.of();
    }
    return repository.findAll(specification);
  }
}
