package gov.va.api.lighthouse.vulcan.mappings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import gov.va.api.lighthouse.vulcan.InvalidRequest;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ReferenceParameterTest {
  private static Stream<Arguments> validParse() {
    return Stream.of(
        Arguments.of("patient", "123", "patient", "123", "123", null),
        Arguments.of("subject", "Patient/123", "Patient", "Patient/123", "123", null),
        Arguments.of("subject:Patient", "123", "Patient", "123", "123", null),
        Arguments.of(
            "patient",
            "https://good.com/Patient/123",
            "Patient",
            "https://good.com/Patient/123",
            "123",
            "https://good.com/Patient/123"),
        Arguments.of(
            "patient",
            "https://good.com/fhir/v0/r4/Patient/123",
            "Patient",
            "https://good.com/fhir/v0/r4/Patient/123",
            "123",
            "https://good.com/fhir/v0/r4/Patient/123"),
        Arguments.of("xxx", "123.456", "xxx", "123.456", "123.456", null));
  }

  @Test
  public void parseBadReferenceUrl() {
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(() -> ReferenceParameter.parse("x", "http://Patient435"));
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> ReferenceParameter.parse("x", "httpq"));
  }

  @Test
  public void parseBlank() {
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(() -> ReferenceParameter.parse("x", ""));
  }

  @Test
  public void parseNull() {
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(() -> ReferenceParameter.parse("x", null));
  }

  @ParameterizedTest
  @MethodSource
  void validParse(
      String parameterName,
      String parameterValue,
      String expectedType,
      String expectedValue,
      String expectedPublicId,
      String expectedUrl) {
    assertThat(ReferenceParameter.parse(parameterName, parameterValue))
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
