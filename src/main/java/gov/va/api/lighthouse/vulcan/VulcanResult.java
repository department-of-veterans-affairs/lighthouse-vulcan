package gov.va.api.lighthouse.vulcan;

import java.util.Optional;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

/*
 * ErrorProne flags the entities stream being included in the toString of the builder for not
 * providing useful information. That's true, but Lombok doesn't seem to have a awy of suppressing
 * including it in the generated builder.
 */
/** The result of executing a request with Vulcan. */
@SuppressWarnings("StreamToString")
@Getter
@Builder
public class VulcanResult<EntityT> {
  @NonNull Stream<EntityT> entities;
  @NonNull Paging paging;

  @Value
  @Builder
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static class Paging {
    long totalRecords;
    int totalPages;
    @NonNull Optional<Integer> firstPage;
    @NonNull Optional<Integer> previousPage;
    @NonNull Optional<Integer> thisPage;
    @NonNull Optional<Integer> nextPage;
    @NonNull Optional<Integer> lastPage;
    @NonNull Optional<String> firstPageUrl;
    @NonNull Optional<String> previousPageUrl;
    @NonNull Optional<String> thisPageUrl;
    @NonNull Optional<String> nextPageUrl;
    @NonNull Optional<String> lastPageUrl;
  }
}
