package gov.va.api.lighthouse.vulcan.mappings;

import static java.util.stream.Collectors.toList;

import gov.va.api.lighthouse.vulcan.Specifications;
import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.jpa.domain.Specification;

@Value
@Builder
@AllArgsConstructor(staticName = "of")
public class Selector<EntityT> {
  private String fieldNameSelector;

  private Object valueSelector;

  /** Build a specification for the given field name and values. */
  public Specification<EntityT> specification() {
    if (valueSelector() instanceof String) {
      return (root, criteriaQuery, criteriaBuilder) ->
          criteriaBuilder.equal(root.get(fieldNameSelector()), valueSelector());
    }
    if (valueSelector() instanceof Collection) {
      Collection<String> values =
          ((Collection<?>) valueSelector())
              .stream()
                  .map(
                      o -> {
                        if (o instanceof String) {
                          return (String) o;
                        }
                        throw new IllegalArgumentException(
                            "Collection must contain only string elements.");
                      })
                  .collect(toList());
      return Specifications.selectInList(fieldNameSelector(), values);
    }
    throw new IllegalArgumentException(
        "Unknown Value Type ("
            + valueSelector().getClass().getSimpleName()
            + "). Please add support to Vulcan.");
  }
}
