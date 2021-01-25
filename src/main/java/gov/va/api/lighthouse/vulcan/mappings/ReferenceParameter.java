package gov.va.api.lighthouse.vulcan.mappings;

import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class ReferenceParameter {
  @NonNull String parameterName;

  @NonNull String value;

  @NonNull String type;

  @NonNull String publicId;

  Optional<String> url;

  /** Lazy getter. */
  public Optional<String> url() {
    if (url == null) {
      url = Optional.empty();
    }
    return url;
  }
}
