package gov.va.api.lighthouse.vulcan;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.UtilityClass;

/** Utilities for specifications. */
@UtilityClass
public class Predicates {

  /** Create a Stream collector for Specifications. */
  public static Collector<Predicate, MatchesAllPredicates, Predicate> andUsing(
      CriteriaBuilder criteriaBuilder) {
    return Collector.of(
        MatchesAllPredicates::new,
        MatchesAllPredicates::add,
        MatchesAllPredicates::add,
        (all) -> criteriaBuilder.and(all.predicates().toArray(new Predicate[0])),
        Characteristics.UNORDERED);
  }

  private static class MatchesAllPredicates {
    @Getter(AccessLevel.PRIVATE)
    List<Predicate> predicates = new ArrayList<>();

    MatchesAllPredicates add(MatchesAllPredicates andUs) {
      predicates().addAll(andUs.predicates());
      return this;
    }

    MatchesAllPredicates add(Predicate andMe) {
      if (andMe == null) {
        return this;
      }
      predicates.add(andMe);
      return this;
    }

    Predicate predicate() {
      return null;
    }
  }
}
