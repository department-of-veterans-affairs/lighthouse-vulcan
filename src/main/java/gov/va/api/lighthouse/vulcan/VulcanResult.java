package gov.va.api.lighthouse.vulcan;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VulcanResult<EntityT> {
  List<EntityT> entities;
}
