package gov.va.api.lighthouse.vulcan.mappings;

import gov.va.api.lighthouse.vulcan.Specifications;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collector;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.springframework.data.jpa.domain.Specification;

// Lombok Generated Builder: redundant cast
@SuppressWarnings("cast")
@Data
@Builder
public class SpecificationSelector<EntityT> {
  @Singular private Map<String, Collection<String>> selectors;

  private Collector<Specification<EntityT>, ?, Specification<EntityT>> collector;

  /** Create a specification using the selectors and collection method. */
  public Specification<EntityT> specification() {
    // When only one selector exists, the collection method is unimportant.
    if (selectors().size() == 1 && collector() == null) {
      collector = Specifications.all();
    }
    if (selectors().isEmpty() || collector() == null) {
      return null;
    }
    return selectors().entrySet().stream()
        .map(
            selector ->
                Specifications.<EntityT>selectInList(selector.getKey(), selector.getValue()))
        .collect(collector());
  }
}
