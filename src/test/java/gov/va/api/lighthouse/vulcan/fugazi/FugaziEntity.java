package gov.va.api.lighthouse.vulcan.fugazi;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "FOO")
public class FugaziEntity {
  @Id @GeneratedValue @Column @EqualsAndHashCode.Include long id;
  @Column @NonNull String name;
  @Column String food;
  @Column @NonNull Instant date;
  @Column long millis;
  @Column String payload;
}
