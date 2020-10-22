package gov.va.api.lighthouse.vulcan;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import org.springframework.data.domain.Sort;

@Value
@Builder
public class VulcanConfiguration<EntityT> {
  @NonNull PagingConfiguration paging;
  @Singular @NonNull List<Mapping<EntityT>> mappings;

  public static <E> VulcanConfigurationBuilder<E> forEntity(
      @SuppressWarnings("unused") Class<E> repo) {
    return VulcanConfiguration.builder();
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
  }
}
