package gov.va.api.lighthouse.vulcan.mappings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import gov.va.api.lighthouse.vulcan.InvalidRequest;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ReferenceParameterParserTest {
  private static Stream<Arguments> validParse() {
    return Stream.of(
        Arguments.of("patient", "123", "Patient", Set.of("Patient"), "Patient", "123", "123", null),
        Arguments.of(
            "subject",
            "Patient/123",
            "Patient",
            Set.of("Patient"),
            "Patient",
            "Patient/123",
            "123",
            null),
        Arguments.of(
            "subject:Patient", "123", "Patient", Set.of("Patient"), "Patient", "123", "123", null),
        Arguments.of(
            "patient",
            "https://good.com/Patient/123",
            "Patient",
            Set.of("Patient"),
            "Patient",
            "https://good.com/Patient/123",
            "123",
            "https://good.com/Patient/123"),
        Arguments.of(
            "patient",
            "https://good.com/fhir/v0/r4/Patient/123",
            "Patient",
            Set.of("Patient"),
            "Patient",
            "https://good.com/fhir/v0/r4/Patient/123",
            "123",
            "https://good.com/fhir/v0/r4/Patient/123"),
        Arguments.of("xxx", "123.456", "Xxx", Set.of("Xxx"), "Xxx", "123.456", "123.456", null),
        Arguments.of(
            "Patient", "123", "Patient", Set.of("Patient"), "Patient", "123", "123", null));
  }

  @Test
  public void parseBadReferenceUrl() {
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                ReferenceParameterParser.builder()
                    .parameterName("x")
                    .parameterValue("http://Patient435")
                    .build()
                    .parse());
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(
            () ->
                ReferenceParameterParser.builder()
                    .parameterName("x")
                    .parameterValue("httpq")
                    .build()
                    .parse());
  }

  @Test
  public void parseBlank() {
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                ReferenceParameterParser.builder()
                    .parameterName("x")
                    .parameterValue("")
                    .build()
                    .parse());
  }

  @Test
  public void parseNull() {
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(() -> ReferenceParameterParser.builder().parameterName("x").build().parse());
  }

  @ParameterizedTest
  @MethodSource
  void validParse(
      String parameterName,
      String parameterValue,
      String defaultResourceType,
      Set<String> allowedReferenceTypes,
      String expectedType,
      String expectedValue,
      String expectedPublicId,
      String expectedUrl) {
    assertThat(
            ReferenceParameterParser.builder()
                .parameterName(parameterName)
                .parameterValue(parameterValue)
                .defaultResourceType(defaultResourceType)
                .allowedReferenceTypes(allowedReferenceTypes)
                .build()
                .parse())
        .isEqualTo(
            ReferenceParameter.builder()
                .parameterName(parameterName)
                .type(expectedType)
                .value(expectedValue)
                .publicId(expectedPublicId)
                .url(expectedUrl)
                .build());
  }
}