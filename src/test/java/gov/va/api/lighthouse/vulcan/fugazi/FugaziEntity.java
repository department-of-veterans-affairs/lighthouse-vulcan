package gov.va.api.lighthouse.vulcan.fugazi;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "FOO")
public class FugaziEntity {
  @Id @GeneratedValue @Column @NotNull @EqualsAndHashCode.Include long id;
  @Column @NotNull String name;
  @Column @NotNull String food;
  @Column @NotNull Instant date;
  @Column long millis;
  @Column String payload;
}
