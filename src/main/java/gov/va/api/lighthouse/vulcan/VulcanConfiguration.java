package gov.va.api.lighthouse.vulcan;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@Value
@Builder
public class VulcanConfiguration<EntityT> {
  @NonNull PagingConfiguration paging;

  ParameterConfiguration parameters;

  @Singular @NonNull List<Mapping<EntityT>> mappings;

  @NonNull Function<HttpServletRequest, Specification<EntityT>> defaultQuery;

  @Singular List<Rule> rules;

  public static <E> VulcanConfigurationBuilder<E> forEntity(
      @SuppressWarnings("unused") Class<E> repo) {
    return VulcanConfiguration.builder();
  }

  private List<String> determineParametersFromMappings() {
    return mappings().stream()
        .flatMap(m -> m.supportedParameterNames().stream())
        .collect(Collectors.toList());
  }

  /** Return the immutable list of rules. */
  public List<Rule> rules() {
    if (rules == null) {
      return List.of();
    }
    return rules;
  }

  /** Return all supported parameters, both learned (from mappings) and given (via builder). */
  public List<String> supportedParameters() {
    if (parameters == null) {
      return determineParametersFromMappings();
    }
    return Stream.concat(
            determineParametersFromMappings().stream(), parameters.parameters().stream())
        .filter(Objects::nonNull)
        .distinct()
        .collect(Collectors.toList());
  }

  @Value
  @Builder
  public static class ParameterConfiguration {
    @Singular @NotEmpty List<String> parameters;
  }

  @Value
  @Builder
  public static class PagingConfiguration {
    @NonNull String pageParameter;

    @NotNull String countParameter;

    @Builder.Default int defaultCount = 10;

    @Builder.Default int maxCount = 20;

    @NotNull Sort sort;

    @NonNull Vulcan.BaseUrlStrategy baseUrlStrategy;

    /** Return true if the given parameter is either the page or count parameter. */
    public boolean isPagingRelatedParameter(String param) {
      return pageParameter().equals(param) || countParameter().equals(param);
    }
  }
}
