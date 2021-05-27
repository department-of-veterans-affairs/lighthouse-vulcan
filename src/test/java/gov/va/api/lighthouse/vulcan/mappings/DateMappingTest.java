package gov.va.api.lighthouse.vulcan.mappings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gov.va.api.lighthouse.vulcan.InvalidRequest;
import gov.va.api.lighthouse.vulcan.fugazi.FugaziEntity;
import gov.va.api.lighthouse.vulcan.mappings.DateMapping.DateFidelity;
import gov.va.api.lighthouse.vulcan.mappings.DateMapping.DateOperator;
import gov.va.api.lighthouse.vulcan.mappings.DateMapping.FixedAmountDateApproximation;
import gov.va.api.lighthouse.vulcan.mappings.DateMapping.SearchableDate;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DateMappingTest {
  static final ZoneOffset localOffset = ZonedDateTime.now(ZoneId.systemDefault()).getOffset();

  static final Function<String, Instant> offset =
      s -> OffsetDateTime.parse(s + localOffset).toInstant();

  @SuppressWarnings("unused")
  static Stream<Arguments> fixedDateApproximation() {
    return Stream.of( // 365 day expansion of each boundary of the range
        arguments(
            "ap2005", // leap year
            offset.apply("2004-01-02T00:00:00.000"),
            offset.apply(
                "2006-12-31T23:59:59.999")), // 30 day expansion of each boundary of the range
        // range is Jan 1 00:00 to 1 ms short of Feb 1.
        arguments(
            "ap2005-01",
            offset.apply("2004-12-02T00:00:00.000"),
            offset.apply(
                "2005-03-02T23:59:59.999")), // 3 day expansion of each boundary of the range
        // range is Jan 21 00:00 to 1 ms short of Jan 22
        arguments(
            "ap2005-01-21",
            offset.apply("2005-01-18T00:00:00.000"),
            offset.apply(
                "2005-01-24T23:59:59.999")), // 1 day expansion of each boundary of the range
        // range is 07:57:00.000 to 07:57:00.999
        arguments(
            "ap2005-01-21T07:57:00Z",
            Instant.parse("2005-01-20T07:57:00.000Z"),
            Instant.parse("2005-01-22T07:57:00.999Z")));
  }

  @SuppressWarnings("unused")
  static Stream<Arguments> parsedParameters() {
    return Stream.of( // year range if only year is specified
        arguments(
            "eq2005",
            DateOperator.EQ,
            offset.apply("2005-01-01T00:00:00.000"),
            offset.apply("2005-12-31T23:59:59.999"),
            DateFidelity.YEAR),
        arguments(
            "2005",
            DateOperator.EQ,
            offset.apply("2005-01-01T00:00:00.000"),
            offset.apply("2005-12-31T23:59:59.999"),
            DateFidelity.YEAR),
        arguments(
            "le2005",
            DateOperator.LE,
            offset.apply("2005-01-01T00:00:00.000"),
            offset.apply("2005-12-31T23:59:59.999"),
            DateFidelity.YEAR),
        arguments(
            "sa2005",
            DateOperator.SA,
            offset.apply("2005-01-01T00:00:00.000"),
            offset.apply("2005-12-31T23:59:59.999"),
            DateFidelity.YEAR),
        arguments(
            "ap2005",
            DateOperator.AP,
            offset.apply("2005-01-01T00:00:00.000"),
            offset.apply("2005-12-31T23:59:59.999"),
            DateFidelity.YEAR), // month range if month is specified
        arguments(
            "ne2005-02",
            DateOperator.NE,
            offset.apply("2005-02-01T00:00:00"),
            offset.apply("2005-02-28T23:59:59.999"),
            DateFidelity.MONTH), // day range if day is specified
        arguments(
            "gt2005-01-21",
            DateOperator.GT,
            offset.apply("2005-01-21T00:00:00"),
            offset.apply("2005-01-21T23:59:59.999"),
            DateFidelity.DAY), // 1 millis range if fully specified
        arguments(
            "lt2005-01-21T07:57:03Z",
            DateOperator.LT,
            Instant.parse("2005-01-21T07:57:03.000Z"),
            Instant.parse("2005-01-21T07:57:03.999Z"),
            DateFidelity.LESS_THAN_A_DAY),
        arguments(
            "ge2005-01-21T07:57:03-04:00",
            DateOperator.GE,
            Instant.parse("2005-01-21T07:57:03.000-04:00"),
            Instant.parse("2005-01-21T07:57:03.999-04:00"),
            DateFidelity.LESS_THAN_A_DAY));
  }

  @ParameterizedTest
  @MethodSource
  void fixedDateApproximation(String parameterValue, Instant lowerBound, Instant upperBound) {
    var ap = DateMapping.defaultGraduatedApproximation();
    var s = new SearchableDate("x", parameterValue);
    assertThat(ap.expandLowerBound(s)).as("lowerbound").isEqualTo(lowerBound);
    assertThat(ap.expandUpperBound(s)).as("upperbound").isEqualTo(upperBound);
  }

  @Test
  void fixedDateApproximationThrowsExceptionWhenNotConfigured() {
    var ap =
        FixedAmountDateApproximation.builder()
            .amounts(Map.of(DateFidelity.YEAR, Duration.ofDays(1)))
            .build();
    var s = new SearchableDate("x", "ap2005-01");
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> ap.expandLowerBound(s));
  }

  @ParameterizedTest
  @MethodSource
  void parsedParameters(
      String parameterValue,
      DateOperator op,
      Instant lowerBound,
      Instant upperBound,
      DateFidelity fidelity) {
    var sd = new SearchableDate("x", parameterValue);
    assertThat(sd.operator()).isEqualTo(op);
    assertThat(sd.fidelity()).isEqualTo(fidelity);
    assertThat(sd.lowerBound()).as("lowerbound").isEqualTo(lowerBound);
    assertThat(sd.upperBound()).as("upperbound").isEqualTo(upperBound);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "x",
        "eq",
        "xx2005",
        "g2005",
        "ggt2005",
        "eq200",
        "eq199X",
        "eq2005-1",
        "eq2005-99",
        "eq2005-01-2",
        "eq2005-01-99",
        "eq2005-01-21T",
        "eq2005-01-21T07",
        "eq2005-01-21T07:5",
        "eq2005-01-21T07:57",
        "eq2005-01-21T07:57:0",
        "eq2005-01-21T07:57:03",
        "eq2005-01-21T99:57:00Z",
        "eq2005-01-21T07:99:00Z",
        "eq2005-01-21T00:00:99Z",
        "eq2005-01-21T07:57:03-",
        "eq2005-01-21T07:57:03-0",
        "eq2005-01-21T07:57:03-04",
        "eq2005-01-21T07:57:03-04:",
        "eq2005-01-21T07:57:03-04:0",
        "eq2005-01-21T07:57:03-0X:00"
      })
  @NullAndEmptySource
  void parsedParametersThrowExceptionForIllegalValues(String parameterValue) {
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(() -> new SearchableDate("x", parameterValue));
  }

  @Test
  void specificationForThrowsExceptionIfParameterIsRepeatedMoreThanTwice() {
    var r = mock(HttpServletRequest.class);
    when(r.getParameterValues("date")).thenReturn(new String[] {"1", "2", "3"});
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                DateMapping.<FugaziEntity, Long>builder()
                    .parameterName("date")
                    .fieldName("x")
                    .predicates((date, field, cb) -> null)
                    .build()
                    .specificationFor(r));
  }
}
