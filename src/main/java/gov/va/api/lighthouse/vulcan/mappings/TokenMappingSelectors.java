package gov.va.api.lighthouse.vulcan.mappings;

import gov.va.api.lighthouse.vulcan.Specifications;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.jpa.domain.Specification;

// Lombok Generated Builder: redundant cast
@SuppressWarnings("cast")
@Data
@Builder
public class TokenMappingSelectors<EntityT> {
  private Map<String, Collection<String>> selectors;

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

  public static class TokenMappingSelectorsBuilder<EntityT> {
    /**
     * Manual creation of Lombok @Singular, Lombok doesn't support overloading/adding methods these
     * methods.
     */
    public TokenMappingSelectorsBuilder<EntityT> selector(
        String selectorKey, Collection<String> selectorValue) {
      if (this.selectors == null) {
        this.selectors = new HashMap<>();
      }
      this.selectors.put(selectorKey, selectorValue);
      return this;
    }

    public TokenMappingSelectorsBuilder<EntityT> selector(
        String selectorKey, String selectorValue) {
      return selector(selectorKey, List.of(selectorValue));
    }
  }
}
