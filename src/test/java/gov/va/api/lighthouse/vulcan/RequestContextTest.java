package gov.va.api.lighthouse.vulcan;

import static gov.va.api.lighthouse.vulcan.Vulcan.useRequestUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gov.va.api.lighthouse.vulcan.Vulcan.PagingParameters;
import gov.va.api.lighthouse.vulcan.fugazi.FugaziEntity;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Sort;

class RequestContextTest {

  @SuppressWarnings("unused")
  static Stream<Arguments> pageAndCount() {
    return Stream.of(
        arguments(null, null, 1, 10),
        arguments("", "", 1, 10),
        arguments("1", "11", 1, 11),
        arguments("2", "20", 2, 20));
  }

  private VulcanConfiguration<FugaziEntity> config() {
    return VulcanConfiguration.forEntity(FugaziEntity.class)
        .paging(
            PagingParameters.builder()
                .pageParameter("page")
                .countParameter("count")
                .defaultCount(10)
                .maxCount(20)
                .sort(Sort.unsorted())
                .baseUrlStrategy(useRequestUrl())
                .build())
        .mappings(Mappings.forEntity(FugaziEntity.class).string("name").get())
        .build();
  }

  RequestContext<FugaziEntity> context(String page, String count) {
    var config = config();
    HttpServletRequest req = mock(HttpServletRequest.class);
    when(req.getParameter("count")).thenReturn(count);
    when(req.getParameter("page")).thenReturn(page);
    return RequestContext.forConfig(config).request(req).build();
  }

  @ParameterizedTest
  @ValueSource(strings = {"-1", "21", "nope"})
  void exceptionIsThrownForInvalidCount(String count) {
    assertThatExceptionOfType(InvalidParameter.class).isThrownBy(() -> context("1", count));
  }

  @ParameterizedTest
  @ValueSource(strings = {"-1", "0", "nope"})
  void exceptionIsThrownForInvalidPage(String page) {
    assertThatExceptionOfType(InvalidParameter.class).isThrownBy(() -> context(page, "10"));
  }

  @ParameterizedTest
  @MethodSource
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
}
