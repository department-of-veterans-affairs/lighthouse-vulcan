package gov.va.api.lighthouse.vulcan.mappings;

import gov.va.api.lighthouse.vulcan.Specifications;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.springframework.data.jpa.domain.Specification;

@Value
@Builder
@AllArgsConstructor
public class SelectorSpecificationCollector<EntityT> {
  @Singular private Collection<Selector<EntityT>> andSelectors;

  @Singular private Collection<Selector<EntityT>> orSelectors;

  /** Create for a single selector. */
  @Builder(builderMethodName = "singleSelectorBuilder", builderClassName = "SingleSelectorBuilder")
  public SelectorSpecificationCollector(Selector<EntityT> selector) {
    this.andSelectors = List.of(selector);
    this.orSelectors = List.of();
  }

  public static SelectorSpecificationCollector<?> empty() {
    return SelectorSpecificationCollector.builder().build();
  }

  /** Build a specification by collecting the Selectors using a SQL AND. */
  public Specification<EntityT> and() {
    if (andSelectors().isEmpty()) {
      return null;
    }
    return andSelectors().stream().map(Selector::specification).collect(Specifications.all());
  }

  /** Build a specification by collecting the Selectors using a SQL OR. */
  public Specification<EntityT> or() {
    if (orSelectors().isEmpty()) {
      return null;
    }
    return orSelectors().stream().map(Selector::specification).collect(Specifications.any());
  }

  /** Build a specification by unifying the AND clauses as well as the ORs. */
  public Specification<EntityT> unify() {
    var specification = and();
    return specification == null ? or() : specification.or(or());
  }
}
