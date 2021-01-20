package gov.va.api.lighthouse.vulcan.mappings;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReferenceParameter {
  String parameterName;

  String value;

  String type;

  String publicId;

  String url;
}
