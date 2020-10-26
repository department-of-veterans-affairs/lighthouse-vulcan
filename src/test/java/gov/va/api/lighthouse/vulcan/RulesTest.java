package gov.va.api.lighthouse.vulcan;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class RulesTest {

  @Test
  void atLeastOneParameterOf() {
    Rules.atLeastOneParameterOf("foo", "bar").check(requestWithParameters("foo"));
    Rules.atLeastOneParameterOf("foo", "bar").check(requestWithParameters("bar"));
    Rules.atLeastOneParameterOf("foo", "bar").check(requestWithParameters("foo", "bar"));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () -> Rules.atLeastOneParameterOf("foo", "bar").check(requestWithParameters("nope")));
  }

  @Test
  void forbiddenParameters() {
    Rules.forbiddenParameters("foo", "bar").check(requestWithParameters("whatever"));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () -> Rules.forbiddenParameters("foo", "bar").check(requestWithParameters("foo")));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () -> Rules.forbiddenParameters("foo", "bar").check(requestWithParameters("bar")));
  }

  @Test
  void ifParameterThenAlsoAtLeastOneParameterOf() {
    Rules.ifParameter("foo")
        .thenAlsoAtLeastOneParameterOf("bar", "ack")
        .check(requestWithParameters("whatever"));
    Rules.ifParameter("foo")
        .thenAlsoAtLeastOneParameterOf("bar", "ack")
        .check(requestWithParameters("foo", "bar"));
    Rules.ifParameter("foo")
        .thenAlsoAtLeastOneParameterOf("bar", "ack")
        .check(requestWithParameters("foo", "bar", "ack"));
    Rules.ifParameter("foo")
        .thenAlsoAtLeastOneParameterOf("bar", "ack")
        .check(requestWithParameters("foo", "ack", "whatever"));
    Rules.ifParameter("foo")
        .thenAlsoAtLeastOneParameterOf("bar", "ack")
        .check(requestWithParameters("foo", "ack"));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                Rules.ifParameter("foo")
                    .thenAlsoAtLeastOneParameterOf("bar", "ack")
                    .check(requestWithParameters("foo", "whatever")));
  }

  @Test
  void ifParameterThenForbidParameters() {
    Rules.ifParameter("foo")
        .thenForbidParameters("bar", "ack")
        .check(requestWithParameters("whatever"));
    Rules.ifParameter("foo")
        .thenForbidParameters("bar", "ack")
        .check(requestWithParameters("foo", "whatever"));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                Rules.ifParameter("foo")
                    .thenForbidParameters("bar", "ack")
                    .check(requestWithParameters("foo", "bar")));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                Rules.ifParameter("foo")
                    .thenForbidParameters("bar", "ack")
                    .check(requestWithParameters("foo", "ack")));
  }

  @Test
  void parametersAlwaysSpecifiedTogether() {
    Rules.parametersAlwaysSpecifiedTogether("foo", "bar").check(requestWithParameters("whatever"));
    Rules.parametersAlwaysSpecifiedTogether("foo", "bar")
        .check(requestWithParameters("foo", "bar"));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                Rules.parametersAlwaysSpecifiedTogether("foo", "bar")
                    .check(requestWithParameters("foo")));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                Rules.parametersAlwaysSpecifiedTogether("foo", "bar")
                    .check(requestWithParameters("bar")));
  }

  @Test
  void parametersNeverSpecifiedTogether() {
    Rules.parametersNeverSpecifiedTogether("foo", "bar").check(requestWithParameters("whatever"));
    Rules.parametersNeverSpecifiedTogether("foo", "bar").check(requestWithParameters("foo"));
    Rules.parametersNeverSpecifiedTogether("foo", "bar").check(requestWithParameters("bar"));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                Rules.parametersNeverSpecifiedTogether("foo", "bar")
                    .check(requestWithParameters("foo", "bar")));
  }

  private MockHttpServletRequest requestWithParameters(String... parameters) {
    MockHttpServletRequest req = new MockHttpServletRequest();
    for (String p : parameters) {
      req.addParameter(p, "value of " + p);
    }
    return req;
  }
}
