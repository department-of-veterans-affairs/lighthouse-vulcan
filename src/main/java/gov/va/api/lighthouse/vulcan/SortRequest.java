package gov.va.api.lighthouse.vulcan;

import java.util.List;
import org.springframework.data.domain.Sort;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public final class SortRequest {
  @NonNull List<Parameter> sorting;

  @Value
  @Builder
  public static final class Parameter {
    @NonNull String parameterName;

    @NonNull Sort.Direction direction;
  }
}
