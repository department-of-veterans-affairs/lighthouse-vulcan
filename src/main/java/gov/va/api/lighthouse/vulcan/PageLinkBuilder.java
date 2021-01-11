package gov.va.api.lighthouse.vulcan;

import static java.util.Map.Entry.comparingByKey;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.joining;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

class PageLinkBuilder {
  private final RequestContext<?> context;
  private final String urlWithOutPaging;

  private PageLinkBuilder(RequestContext<?> context) {
    this.context = context;
    urlWithOutPaging = determineUrlWithoutPaging();
  }

  public static PageLinkBuilder of(RequestContext<?> context) {
    return new PageLinkBuilder(context);
  }

  private Stream<String> asQueryParameters(Map.Entry<String, String[]> entry) {
    return Stream.of(entry.getValue())
        .map((value) -> entry.getKey() + '=' + URLEncoder.encode(value, StandardCharsets.UTF_8));
  }

  private String determineUrlWithoutPaging() {
    StringBuilder url =
        new StringBuilder(context.config().paging().baseUrlStrategy().apply(context.request()))
            .append('?');
    Map<String, String[]> parameters = context.request().getParameterMap();
    List<String> allowedParameters = context.config().allowedParameters();
    String queryString =
        parameters.entrySet().stream()
            .filter(entry -> !context.config().paging().isPagingRelatedParameter(entry.getKey()))
            .filter(entry -> allowedParameters.contains(entry.getKey()))
            .sorted(comparingByKey())
            .flatMap(this::asQueryParameters)
            .collect(joining("&"));
    if (!queryString.isEmpty()) {
      url.append(queryString).append('&');
    }
    return url.toString();
  }

  public Optional<String> urlForPage(Integer page) {
    if (page == null) {
      return empty();
    }
    return Optional.of(
        urlWithOutPaging
            + (context.config().paging().countParameter() + '=' + context.count())
            + '&'
            + (context.config().paging().pageParameter() + '=' + page));
  }
}
