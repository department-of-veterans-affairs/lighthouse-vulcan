package gov.va.api.lighthouse.vulcan;

import java.util.Collection;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import javax.persistence.criteria.CriteriaBuilder.In;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

/** Utilities for specifications. */
@UtilityClass
public class Specifications {
  /**
   * Create a Stream collector for Specifications where the final form must match all specifications
   * that were collected.
   */
  public static <E>
      Collector<Specification<E>, MatchesAllSpecifications<E>, Specification<E>> all() {
    return Collector.of(
        MatchesAllSpecifications::new,
        MatchesAllSpecifications::add,
        (a, b) -> a.add(b.specification()),
        MatchesAllSpecifications::specification,
        Characteristics.UNORDERED);
  }

  /**
   * Create a Stream collector for Specifications where the final form must match any specifications
   * that were collected.
   */
  public static <E>
      Collector<Specification<E>, MatchesAnySpecifications<E>, Specification<E>> any() {
    return Collector.of(
        MatchesAnySpecifications::new,
        MatchesAnySpecifications::add,
        (a, b) -> a.add(b.specification()),
        MatchesAnySpecifications::specification,
        Characteristics.UNORDERED);
  }

  /** Produces a specification than explicitly handles a lists of 0 and 1. */
  public static <E> Specification<E> selectInList(String fieldName, Collection<String> values) {
    if (values == null || values.isEmpty()) {
      return null;
    }
    if (values.size() == 1) {
      return (root, criteriaQuery, criteriaBuilder) ->
          criteriaBuilder.equal(root.get(fieldName), values.stream().findFirst().orElseThrow());
    }
    return (root, criteriaQuery, criteriaBuilder) -> {
      In<String> in = criteriaBuilder.in(root.get(fieldName));
      values.forEach(in::value);
      return criteriaBuilder.or(in);
    };
  }

  private static class MatchesAllSpecifications<E> {
    @Getter Specification<E> specification;

    MatchesAllSpecifications<E> add(Specification<E> andMe) {
      if (andMe != null) {
        if (specification == null) {
          specification = andMe;
        } else {
          specification = specification.and(andMe);
        }
      }
      return this;
    }
  }

  private static class MatchesAnySpecifications<E> {
    @Getter Specification<E> specification;

    MatchesAnySpecifications<E> add(Specification<E> andMe) {
      if (andMe != null) {
        if (specification == null) {
          specification = andMe;
        } else {
          specification = specification.or(andMe);
        }
      }
      return this;
    }
  }
}
