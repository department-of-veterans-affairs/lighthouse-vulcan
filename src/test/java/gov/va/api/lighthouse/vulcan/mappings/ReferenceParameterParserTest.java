package gov.va.api.lighthouse.vulcan.mappings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import gov.va.api.lighthouse.vulcan.InvalidRequest;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ReferenceParameterParserTest {
  private static Stream<Arguments> invalidRequest() {
    /*
    Arguments.of(parameterName, parameterValue, defaultResourceType, allowedReferenceTypes)
    */
    return Stream.of(
        Arguments.of("recorder", "123", "X", Set.of("Organization", "Practitioner")),
        Arguments.of("recorder", "123", "Organization", Set.of("Practitioner")),
        Arguments.of("x", "", "X", Set.of("X")),
        Arguments.of("x", "http://Patient435", "x", Set.of("X")),
        Arguments.of("param$ter", "123", "X", Set.of("X")),
        Arguments.of("trash", "%%#@%@", "trash", Set.of("trash")),
        Arguments.of("yelling", "1-2-3-4-5-6-7!!", "yelling", Set.of("yelling")));
  }

  private static Stream<Arguments> validParse() {
    /*
    Arguments.of(parameterName, parameterValue, defaultResourceType, allowedReferenceTypes,
    expectedType, expectedValue, expectedPublicId, expectedUrl)
    */
    return Stream.of(
        Arguments.of(
            "patient",
            "123",
            "Patient",
            Set.of("Patient"),
            "Patient",
            "123",
            "123",
            Optional.empty()),
        Arguments.of(
            "subject",
            "Patient/123",
            "Patient",
            Set.of("Patient"),
            "Patient",
            "Patient/123",
            "123",
            Optional.empty()),
        Arguments.of(
            "subject:Patient",
            "123",
            "Patient",
            Set.of("Patient"),
            "Patient",
            "123",
            "123",
            Optional.empty()),
        Arguments.of(
            "patient",
            "https://good.com/Patient/123",
            "Patient",
            Set.of("Patient"),
            "Patient",
            "https://good.com/Patient/123",
            "123",
            Optional.of("https://good.com/Patient/123")),
        Arguments.of(
            "patient",
            "https://good.com/fhir/v0/r4/Patient/123",
            "Patient",
            Set.of("Patient"),
            "Patient",
            "https://good.com/fhir/v0/r4/Patient/123",
            "123",
            Optional.of("https://good.com/fhir/v0/r4/Patient/123")),
        Arguments.of(
            "xxx", "123.456", "Xxx", Set.of("Xxx"), "Xxx", "123.456", "123.456", Optional.empty()),
        Arguments.of(
            "Patient",
            "123",
            "Patient",
            Set.of("Patient"),
            "Patient",
            "123",
            "123",
            Optional.empty()),
        Arguments.of(
            "recorder:Organization",
            "123",
            "Organization",
            Set.of("Organization", "Practitioner"),
            "Organization",
            "123",
            "123",
            Optional.empty()),
        Arguments.of(
            "recorder:Practitioner",
            "123",
            "Organization",
            Set.of("Organization", "Practitioner"),
            "Practitioner",
            "123",
            "123",
            Optional.empty()));
  }

  @ParameterizedTest
  @MethodSource
  void invalidRequest(
      String parameterName,
      String parameterValue,
      String defaultResourceType,
      Set<String> allowedResourceTypes) {
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                ReferenceParameterParser.builder()
                    .parameterName(parameterName)
                    .parameterValue(parameterValue)
                    .allowedReferenceTypes(allowedResourceTypes)
                    .formats(
                        ReferenceParameterParser.standardFormatsForResource(
                            defaultResourceType, allowedResourceTypes))
                    .build()
                    .parse());
  }

  @Test
  void standardFormatsThrowsNpeWhenParametersAreNotProvided() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> ReferenceParameterParser.standardFormatsForResource(null, Set.of("X")));
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> ReferenceParameterParser.standardFormatsForResource("x", null));
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
      Optional<String> expectedUrl) {
    assertThat(
            ReferenceParameterParser.builder()
                .parameterName(parameterName)
                .parameterValue(parameterValue)
                .allowedReferenceTypes(allowedReferenceTypes)
                .formats(
                    ReferenceParameterParser.standardFormatsForResource(
                        defaultResourceType, allowedReferenceTypes))
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
