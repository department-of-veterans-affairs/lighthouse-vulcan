package gov.va.api.lighthouse.vulcan;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;

@Value
@Builder
public final class SortRequest {
  @NonNull @Builder.Default List<Parameter> sorting = List.of();

  /** Create from a comma-delimited list of rules, e.g. status,-date,category. */
  public static SortRequest forCsv(@NonNull String csv) {
    List<Parameter> parameters =
        Arrays.stream(csv.split(",", -1))
            .map(StringUtils::trimToNull)
            .filter(Objects::nonNull)
            .map(
                p -> {
                  if (p.startsWith("-")) {
                    return Parameter.builder()
                        .parameterName(p.substring(1))
                        .direction(Sort.Direction.DESC)
                        .build();
                  } else {
                    return Parameter.builder()
                        .parameterName(p)
                        .direction(Sort.Direction.ASC)
                        .build();
                  }
                })
            .collect(toList());
    return builder().sorting(parameters).build();
  }

  @Value
  @Builder
  public static final class Parameter {
    @NonNull String parameterName;

    @NonNull Sort.Direction direction;
  }
}
