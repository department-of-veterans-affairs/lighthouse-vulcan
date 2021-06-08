package gov.va.api.lighthouse.vulcan;

import static gov.va.api.lighthouse.vulcan.Vulcan.returnNothing;
import static gov.va.api.lighthouse.vulcan.Vulcan.useRequestUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gov.va.api.lighthouse.vulcan.VulcanConfiguration.PagingConfiguration;
import gov.va.api.lighthouse.vulcan.fugazi.FugaziEntity;
import gov.va.api.lighthouse.vulcan.mappings.Mappings;
import java.util.List;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

class RequestContextTest {
  private static VulcanConfiguration<FugaziEntity> config() {
    return VulcanConfiguration.forEntity(FugaziEntity.class)
        .paging(
            PagingConfiguration.builder()
                .pageParameter("page")
                .countParameter("count")
                .defaultCount(10)
                .maxCount(20)
                .sortDefault(Sort.unsorted())
                .sortableParameters(r -> null)
                .baseUrlStrategy(useRequestUrl())
                .build())
        .mappings(Mappings.forEntity(FugaziEntity.class).string("name").get())
        .defaultQuery(returnNothing())
        .build();
  }

  static RequestContext<FugaziEntity> context(String page, String count) {
    var config = config();
    HttpServletRequest req = mock(HttpServletRequest.class);
    when(req.getParameter("count")).thenReturn(count);
    when(req.getParameter("page")).thenReturn(page);
    return RequestContext.forConfig(config).request(req).build();
  }

  @SuppressWarnings("unused")
  static Stream<Arguments> pageAndCount() {
    return Stream.of(
        arguments(null, null, 1, 10),
        arguments("", "", 1, 10),
        arguments("1", "11", 1, 11),
        arguments("2", "20", 2, 20),
        arguments("2", "21", 2, 20));
  }

  @Test
  void defaultQueryIsUsedIfNoSpecificationsAreBuiltFromTheRequestParameters() {
    @SuppressWarnings("unchecked")
    Specification<FugaziEntity> specification = mock(Specification.class);
    var config =
        VulcanConfiguration.forEntity(FugaziEntity.class)
            .paging(
                PagingConfiguration.builder()
                    .pageParameter("page")
                    .countParameter("count")
                    .defaultCount(10)
                    .maxCount(20)
                    .sortDefault(Sort.unsorted())
                    .baseUrlStrategy(useRequestUrl())
                    .build())
            .mappings(Mappings.forEntity(FugaziEntity.class).string("name").get())
            .defaultQuery(r -> specification)
            .build();
    HttpServletRequest req = mock(HttpServletRequest.class);
    assertThat(RequestContext.forConfig(config).request(req).build().specification())
        .isSameAs(specification);
  }

  @ParameterizedTest
  @ValueSource(strings = {"-1", "nope"})
  void exceptionIsThrownForInvalidCount(String count) {
    assertThatExceptionOfType(InvalidRequest.class).isThrownBy(() -> context("1", count));
  }

  @ParameterizedTest
  @ValueSource(strings = {"-1", "0", "nope"})
  void exceptionIsThrownForInvalidPage(String page) {
    assertThatExceptionOfType(InvalidRequest.class).isThrownBy(() -> context(page, "10"));
  }

  @MethodSource
  @ParameterizedTest
  void pageAndCount(String page, String count, int expectedPage, int expectedCount) {
    var ctx = context(page, count);
    assertThat(ctx.page()).as("page").isEqualTo(expectedPage);
    assertThat(ctx.count()).as("count").isEqualTo(expectedCount);
    // Request paging is 1 based, Database paging is 0 based.
    assertThat(ctx.pageRequest().getPageNumber())
        .as("page request page number")
        .isEqualTo(expectedPage - 1);
    assertThat(ctx.pageRequest().getPageSize())
        .as("page request page size")
        .isEqualTo(expectedCount);
  }

  @Test
  void rulesWillRejectTheRequest() {
    @SuppressWarnings("unchecked")
    Specification<FugaziEntity> specification = mock(Specification.class);
    var config =
        VulcanConfiguration.forEntity(FugaziEntity.class)
            .paging(
                PagingConfiguration.builder()
                    .pageParameter("page")
                    .countParameter("count")
                    .defaultCount(10)
                    .maxCount(20)
                    .sortDefault(Sort.unsorted())
                    .baseUrlStrategy(useRequestUrl())
                    .build())
            .mappings(Mappings.forEntity(FugaziEntity.class).string("name").get())
            .defaultQuery(returnNothing())
            .rules(
                List.of(
                    r -> {},
                    r -> {},
                    r -> {
                      throw new InvalidRequest("fugazi");
                    }))
            .build();
    HttpServletRequest req = mock(HttpServletRequest.class);
    assertThatExceptionOfType(InvalidRequest.class)
        .isThrownBy(() -> RequestContext.forConfig(config).request(req).build());
  }
}
