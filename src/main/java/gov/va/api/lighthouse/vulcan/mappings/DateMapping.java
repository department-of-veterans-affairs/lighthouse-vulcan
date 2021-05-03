package gov.va.api.lighthouse.vulcan.mappings;

import static gov.va.api.lighthouse.vulcan.Predicates.andUsing;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import gov.va.api.lighthouse.vulcan.InvalidRequest;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString.Exclude;
import lombok.Value;
import org.springframework.data.jpa.domain.Specification;

/**
 * Provides parameter mapping for FHIR string type searches. See
 * http://hl7.org/fhir/R4/search.html#date and http://hl7.org/fhir/R4/search.html#prefix
 *
 * <p>This provides pluggable support for different JPA entity field types, e.g. long vs Instant. It
 * also provides pluggable support for date approximation when "ap" search prefix is used, e.g.
 * "ap2005-01-21"
 */
@Value
@Builder
public class DateMapping<EntityT, DateT> implements SingleParameterMapping<EntityT> {
  String parameterName;

  String fieldName;

  @Exclude PredicateFactory<DateT> predicates;

  /**
   * Default date approximate will used fixed amounts that increased based on the fidelity of the
   * search. That is the search date range is larger for more general searches, e.g. ap2005 will
   * search a much larger range than ap2005-01-21
   */
  public static DateApproximation defaultGraduatedApproximation() {
    return FixedAmountDateApproximation.builder()
        .amount(DateFidelity.YEAR, Duration.ofDays(365))
        .amount(DateFidelity.MONTH, Duration.ofDays(30))
        .amount(DateFidelity.DAY, Duration.ofDays(3))
        .amount(DateFidelity.LESS_THAN_A_DAY, Duration.ofDays(1))
        .build();
  }

  @Override
  public Specification<EntityT> specificationFor(HttpServletRequest request) {
    String[] dates = request.getParameterValues(parameterName());
    if (dates.length > 2) {
      throw InvalidRequest.repeatedTooManyTimes(parameterName(), 2, dates.length);
    }
    List<SearchableDate> searchableDates =
        Stream.of(dates).map(v -> new SearchableDate(parameterName(), v)).collect(toList());
    return (root, criteriaQuery, criteriaBuilder) -> {
      Path<DateT> field = root.get(fieldName());
      return searchableDates
          .stream()
          .map(sd -> predicates().predicate(sd, field, criteriaBuilder))
          .collect(andUsing(criteriaBuilder));
    };
  }

  /** FHIR date prefixes or date operations. */
  public enum DateOperator {
    EQ,
    NE,
    GT,
    LT,
    GE,
    LE,
    SA,
    EB,
    AP
  }

  /**
   * Indicator on how much fidelity a search was made for approximate dates, e.g. ap2005 vs
   * ap2005-01-021
   */
  public enum DateFidelity {
    YEAR,
    MONTH,
    DAY,
    LESS_THAN_A_DAY
  }

  /**
   * Pluggable field predicate generation. This is where you support different JPA entity field
   * types, such as Instant or long.
   */
  @FunctionalInterface
  public interface PredicateFactory<FieldT> {
    Predicate predicate(
        SearchableDate date, Expression<? extends FieldT> field, CriteriaBuilder criteriaBuilder);
  }

  /** Pluggable date approximation. */
  public interface DateApproximation {
    /**
     * Return a revised version of the lower bound that has been expanded to account for
     * approximation.
     */
    Instant expandLowerBound(SearchableDate date);

    /**
     * Return a revised version of the upper bound that has been expanded to account for
     * approximation.
     */
    Instant expandUpperBound(SearchableDate date);
  }

  /**
   * Date approximation that expands the lower and upper bounds a fixed amount based on the fidelity
   * of the search. Instances MUST be configured with a duration for each fidelity.
   */
  @Value
  @Builder
  // thanks lombok
  @SuppressWarnings("cast")
  public static class FixedAmountDateApproximation implements DateApproximation {
    @Singular Map<DateFidelity, Duration> amounts;

    private Duration amountFor(DateFidelity fidelity) {
      var amount = amounts().get(fidelity);
      if (amount == null) {
        throw new IllegalStateException("No amount for " + fidelity + " configured.");
      }
      return amount;
    }

    @Override
    public Instant expandLowerBound(SearchableDate date) {
      return date.lowerBound().minus(amountFor(date.fidelity()));
    }

    @Override
    public Instant expandUpperBound(SearchableDate date) {
      return date.upperBound().plus(amountFor(date.fidelity()));
    }
  }

  /** Approximation support for Instant JPA fields with pluggable approximation. */
  @Value
  @Builder
  public static class InstantPredicateFactory implements PredicateFactory<Instant> {
    DateApproximation approximation;

    @SuppressWarnings("EnhancedSwitchMigration")
    @Override
    public Predicate predicate(
        SearchableDate date, Expression<? extends Instant> field, CriteriaBuilder criteriaBuilder) {
      switch (date.operator()) {
        case EQ:
          return criteriaBuilder.and(
              criteriaBuilder.greaterThanOrEqualTo(field, date.lowerBound()),
              criteriaBuilder.lessThanOrEqualTo(field, date.upperBound()));
        case NE:
          return criteriaBuilder.or(
              criteriaBuilder.lessThan(field, date.lowerBound()),
              criteriaBuilder.greaterThan(field, date.upperBound()));
        case GT:
          // fall-through
        case SA:
          return criteriaBuilder.greaterThan(field, date.upperBound());
        case LT:
          // fall-through
        case EB:
          return criteriaBuilder.lessThan(field, date.lowerBound());
        case GE:
          return criteriaBuilder.greaterThanOrEqualTo(field, date.lowerBound());
        case LE:
          return criteriaBuilder.lessThanOrEqualTo(field, date.upperBound());
        case AP:
          return criteriaBuilder.and(
              criteriaBuilder.greaterThanOrEqualTo(field, approximation().expandLowerBound(date)),
              criteriaBuilder.lessThanOrEqualTo(field, approximation().expandUpperBound(date)));
        default:
          throw new InvalidRequest("Unknown date search operator: " + date.operator());
      }
    }
  }

  /**
   * This represents the searchable date value, e.g. createdondate=gt2005. This supports the formats
   * defined by FHIRs date search specification.
   *
   * <p>tl;dr
   *
   * <pre>
   * [${prefix}]${date}
   * Where ${prefix} is defined by the DateOperators and case insensitive.
   * - eq, ne, gt, lt, ge, le, sa, eb, ap
   * And ${date} is in one of the following formats
   * - YYYY
   * - YYYY-MM
   * - YYYY-MM-DD
   * - YYYY-MM-DD'T'HH:MM:SS
   * - YYYY-MM-DD'T'HH:MM:SSZ
   * - YYYY-MM-DD'T'HH:MM:SS-HH:MM
   * - YYYY-MM-DD'T'HH:MM:SS+HH:MM
   * </pre>
   */
  @SuppressWarnings("EnhancedSwitchMigration")
  @Value
  public static class SearchableDate {
    private static final int YEAR = 4;

    private static final int YEAR_MONTH = 7;

    private static final int YEAR_MONTH_DAY = 10;

    private static final int TIME_ZONE = 20;

    private static final int TIME_ZONE_OFFSET = 25;

    String parameterName;

    String operatorAndDate;

    DateOperator operator;

    String date;

    DateFidelity fidelity;

    Instant lowerBound;

    Instant upperBound;

    SearchableDate(String parameterName, String operatorAndDate) {
      this.parameterName = parameterName;
      this.operatorAndDate = operatorAndDate;
      if (isBlank(operatorAndDate) || operatorAndDate.length() <= 1) {
        throw invalidParameterValue();
      }
      if (Character.isLetter(operatorAndDate.charAt(0))) {
        operator = operatorOrDie(operatorAndDate.substring(0, 2));
        date = operatorAndDate.substring(2);
      } else {
        operator = DateOperator.EQ;
        date = operatorAndDate;
      }
      fidelity = computeDateFidelity();
      try {
        lowerBound = computeLowerBound();
        upperBound = computeUpperBound();
      } catch (DateTimeParseException e) {
        throw invalidParameterValue();
      }
    }

    private DateFidelity computeDateFidelity() {
      switch (date().length()) {
        case YEAR:
          return DateFidelity.YEAR;
        case YEAR_MONTH:
          return DateFidelity.MONTH;
        case YEAR_MONTH_DAY:
          return DateFidelity.DAY;
        case TIME_ZONE:
          // falls through
        case TIME_ZONE_OFFSET:
          return DateFidelity.LESS_THAN_A_DAY;
        default:
          throw invalidParameterValue();
      }
    }

    private Instant computeLowerBound() {
      ZoneOffset offset = ZonedDateTime.now(ZoneId.systemDefault()).getOffset();
      switch (date().length()) {
        case YEAR:
          return OffsetDateTime.parse(String.format("%s-01-01T00:00:00%s", date(), offset))
              .toInstant();
        case YEAR_MONTH:
          return OffsetDateTime.parse(String.format("%s-01T00:00:00%s", date(), offset))
              .toInstant();
        case YEAR_MONTH_DAY:
          return OffsetDateTime.parse(String.format("%sT00:00:00%s", date(), offset)).toInstant();
        case TIME_ZONE:
          return Instant.parse(date());
        case TIME_ZONE_OFFSET:
          return OffsetDateTime.parse(date()).toInstant();
        default:
          throw invalidParameterValue();
      }
    }

    private Instant computeUpperBound() {
      OffsetDateTime offsetLowerBound =
          OffsetDateTime.ofInstant(
              lowerBound(), ZonedDateTime.now(ZoneId.systemDefault()).getOffset());
      switch (date().length()) {
        case YEAR:
          return offsetLowerBound.plusYears(1).minus(1, ChronoUnit.MILLIS).toInstant();
        case YEAR_MONTH:
          return offsetLowerBound.plusMonths(1).minus(1, ChronoUnit.MILLIS).toInstant();
        case YEAR_MONTH_DAY:
          return offsetLowerBound.plusDays(1).minus(1, ChronoUnit.MILLIS).toInstant();
        case TIME_ZONE:
          // falls through
        case TIME_ZONE_OFFSET:
          return offsetLowerBound.plusSeconds(1).minus(1, ChronoUnit.MILLIS).toInstant();
        default:
          throw invalidParameterValue();
      }
    }

    private InvalidRequest invalidParameterValue() {
      return InvalidRequest.badParameter(
          parameterName,
          operatorAndDate,
          "Expected: [EQ|NE|GT|LT|GE|LE|SA|EB|AP]YYYY[-MM][-DD]['T'HH:MM:SS][Z|(+|-)HH:MM]");
    }

    private DateOperator operatorOrDie(String value) {
      try {
        return DateOperator.valueOf(value.toUpperCase(Locale.US));
      } catch (IllegalArgumentException e) {
        throw invalidParameterValue();
      }
    }
  }
}
