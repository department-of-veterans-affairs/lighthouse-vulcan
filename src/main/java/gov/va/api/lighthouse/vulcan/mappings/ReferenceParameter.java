package gov.va.api.lighthouse.vulcan.mappings;

import java.util.Optional;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class ReferenceParameter {
  @NonNull String parameterName;

  @NonNull String value;

  @NonNull String type;

  @NonNull String publicId;

  Optional<String> url;
}
