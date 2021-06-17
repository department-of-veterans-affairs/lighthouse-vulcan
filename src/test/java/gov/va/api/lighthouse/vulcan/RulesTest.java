package gov.va.api.lighthouse.vulcan;

import static gov.va.api.lighthouse.vulcan.Vulcan.useRequestUrl;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import gov.va.api.lighthouse.vulcan.VulcanConfiguration.PagingConfiguration;
import gov.va.api.lighthouse.vulcan.fugazi.FugaziEntity;
import gov.va.api.lighthouse.vulcan.mappings.Mappings;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
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
  void atLeastOneParameterOfWithModifiers() {
    Rules.atLeastOneParameterOf("foo", "str").check(requestWithParameters("foo"));
    Rules.atLeastOneParameterOf("foo", "str").check(requestWithParameters("str"));
    Rules.atLeastOneParameterOf("foo", "str").check(requestWithParameters("str:contains"));
    Rules.atLeastOneParameterOf("foo", "str").check(requestWithParameters("str:exact"));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                Rules.atLeastOneParameterOf("foo", "str").check(requestWithParameters("str:nope")));
  }

  @Test
  void forbidUnknownParameters() {
    Rules.forbidUnknownParameters().check(requestWithParameters("foo"));
    Rules.forbidUnknownParameters().check(requestWithParameters("bar"));
    Rules.forbidUnknownParameters().check(requestWithParameters("page"));
    Rules.forbidUnknownParameters().check(requestWithParameters("count"));
    Rules.forbidUnknownParameters().check(requestWithParameters());
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () -> Rules.forbidUnknownParameters().check(requestWithParameters("foo", "nope")));
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
  void forbiddenParametersWithModifiers() {
    Rules.forbiddenParameters("foo", "str").check(requestWithParameters("bar"));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () -> Rules.forbiddenParameters("foo", "str").check(requestWithParameters("str")));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                Rules.forbiddenParameters("foo", "str")
                    .check(requestWithParameters("str:contains")));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                Rules.forbiddenParameters("foo", "str").check(requestWithParameters("str:exact")));
  }

  @Test
  void ifParameterThenAllowOnlyKnownModifiers() {
    var rule = Rules.ifParameter("nacho").thenAllowOnlyKnownModifiers("friday", "libre");
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(() -> rule.check(requestWithParameters("nacho:wednesday")));
    rule.check(requestWithParameters("nacho"));
    rule.check(requestWithParameters("nacho:friday"));
    rule.check(requestWithParameters("nacho:libre"));
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
  void ifParameterThenAlsoAtLeastOneParameterOfWithModifiers() {
    Rules.ifParameter("foo")
        .thenAlsoAtLeastOneParameterOf("str")
        .check(requestWithParameters("whatever"));
    Rules.ifParameter("foo")
        .thenAlsoAtLeastOneParameterOf("str")
        .check(requestWithParameters("foo", "str"));
    Rules.ifParameter("foo")
        .thenAlsoAtLeastOneParameterOf("str")
        .check(requestWithParameters("foo", "str:contains"));
    Rules.ifParameter("foo")
        .thenAlsoAtLeastOneParameterOf("str")
        .check(requestWithParameters("foo", "str:exact"));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                Rules.ifParameter("foo")
                    .thenAlsoAtLeastOneParameterOf("str")
                    .check(requestWithParameters("foo", "whatever")));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                Rules.ifParameter("foo")
                    .thenAlsoAtLeastOneParameterOf("str")
                    .check(requestWithParameters("foo", "str:nope")));
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
  void ifParameterThenForbidParametersWithModifiers() {
    Rules.ifParameter("foo").thenForbidParameters("str").check(requestWithParameters("whatever"));
    Rules.ifParameter("foo")
        .thenForbidParameters("str")
        .check(requestWithParameters("foo", "whatever"));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                Rules.ifParameter("foo")
                    .thenForbidParameters("str")
                    .check(requestWithParameters("foo", "str")));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                Rules.ifParameter("foo")
                    .thenForbidParameters("str")
                    .check(requestWithParameters("foo", "str:contains")));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                Rules.ifParameter("foo")
                    .thenForbidParameters("str")
                    .check(requestWithParameters("foo", "str:exact")));
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
  void parametersAlwaysSpecifiedTogetherWithModifiers() {
    Rules.parametersAlwaysSpecifiedTogether("foo", "str").check(requestWithParameters("whatever"));
    Rules.parametersAlwaysSpecifiedTogether("foo", "str")
        .check(requestWithParameters("foo", "str"));
    Rules.parametersAlwaysSpecifiedTogether("foo", "str")
        .check(requestWithParameters("foo", "str:contains"));
    Rules.parametersAlwaysSpecifiedTogether("foo", "str")
        .check(requestWithParameters("foo", "str:exact"));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                Rules.parametersAlwaysSpecifiedTogether("foo", "str")
                    .check(requestWithParameters("foo")));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                Rules.parametersAlwaysSpecifiedTogether("foo", "str")
                    .check(requestWithParameters("str")));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                Rules.parametersAlwaysSpecifiedTogether("foo", "str")
                    .check(requestWithParameters("str:contains")));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                Rules.parametersAlwaysSpecifiedTogether("foo", "str")
                    .check(requestWithParameters("foo", "str:nope")));
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

  @Test
  void parametersNeverSpecifiedTogetherWithModifiers() {
    Rules.parametersNeverSpecifiedTogether("foo", "str").check(requestWithParameters("whatever"));
    Rules.parametersNeverSpecifiedTogether("foo", "str").check(requestWithParameters("foo"));
    Rules.parametersNeverSpecifiedTogether("foo", "str").check(requestWithParameters("str"));
    Rules.parametersNeverSpecifiedTogether("foo", "str")
        .check(requestWithParameters("str:contains"));
    Rules.parametersNeverSpecifiedTogether("foo", "str").check(requestWithParameters("str:exact"));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                Rules.parametersNeverSpecifiedTogether("foo", "str")
                    .check(requestWithParameters("foo", "str")));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                Rules.parametersNeverSpecifiedTogether("foo", "str")
                    .check(requestWithParameters("foo", "str:contains")));
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(
            () ->
                Rules.parametersNeverSpecifiedTogether("foo", "str")
                    .check(requestWithParameters("foo", "str:exact")));
  }

  private FugaziRuleContext requestWithParameters(String... parameters) {
    var req = new MockHttpServletRequest();
    for (String p : parameters) {
      req.addParameter(p, "value of " + p);
    }
    var config =
        VulcanConfiguration.forEntity(FugaziEntity.class)
            .paging(
                PagingConfiguration.builder()
                    .countParameter("count")
                    .pageParameter("page")
                    .sortDefault(Sort.unsorted())
                    .maxCount(20)
                    .defaultCount(10)
                    .baseUrlStrategy(useRequestUrl())
                    .build())
            .defaultQuery(Vulcan.returnNothing())
            .mappings(
                Mappings.forEntity(FugaziEntity.class)
                    .value("foo")
                    .value("bar")
                    .string("str")
                    .get())
            .build();
    return new FugaziRuleContext(req, config);
  }

  @Value
  @RequiredArgsConstructor
  private static class FugaziRuleContext implements RuleContext {
    HttpServletRequest request;

    VulcanConfiguration<?> config;
  }
}
