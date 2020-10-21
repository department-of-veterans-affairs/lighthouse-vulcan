package gov.va.api.lighthouse.vulcan;

import java.util.Optional;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class VulcanResult<EntityT> {
  @NonNull Stream<EntityT> entities;

  @NonNull Paging paging;

  @Value
  @Builder
  public static class Paging {
    @NonNull long totalRecords;
    @NonNull int totalPages;
    @NonNull Optional<Integer> firstPage;
    @NonNull Optional<Integer> previousPage;
    @NonNull Optional<Integer> thisPage;
    @NonNull Optional<Integer> nextPage;
    @NonNull Optional<Integer> lastPage;
  }
}
