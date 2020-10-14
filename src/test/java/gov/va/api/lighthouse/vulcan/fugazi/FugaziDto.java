package gov.va.api.lighthouse.vulcan.fugazi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FugaziDto {
  private String name;
  private int favoriteNumber;
  private Food dinner;

  public enum Food {
    NACHOS,
    TACOS,
    EVEN_MORE_NACHOS
  }
}
