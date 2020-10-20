package gov.va.api.lighthouse.vulcan;

import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class VulcanConfiguration<EntityT> {
  @NonNull Vulcan.PagingParameters paging;
  @Singular @NonNull List<Mapping<EntityT>> mappings;

  public static <E> VulcanConfigurationBuilder<E> forEntity(Class<E> repo) {
    return VulcanConfiguration.<E>builder();
  }
}
