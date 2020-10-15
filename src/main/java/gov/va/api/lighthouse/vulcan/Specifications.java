package gov.va.api.lighthouse.vulcan;

import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

/** Utilities for specifications. */
@UtilityClass
public class Specifications {

  /** Create a Stream collector for Specifications. */
  public static <E>
      Collector<Specification<E>, MatchesAllSpecifications<E>, Specification<E>> and() {
    return Collector.of(
        MatchesAllSpecifications::new,
        MatchesAllSpecifications::add,
        (a, b) -> a.add(b.specification()),
        MatchesAllSpecifications::specification,
        Characteristics.UNORDERED);
  }

  private static class MatchesAllSpecifications<E> {
    @Getter Specification<E> specification;

    MatchesAllSpecifications<E> add(Specification<E> andMe) {
      if (andMe == null) {
        return this;
      }
      if (specification == null) {
        specification = andMe;
      } else {
        specification = specification.and(andMe);
      }
      return this;
    }
  }
}
