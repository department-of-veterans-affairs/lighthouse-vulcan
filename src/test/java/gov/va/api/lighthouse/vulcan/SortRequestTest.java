package gov.va.api.lighthouse.vulcan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

public class SortRequestTest {
  private static final Sort.Direction ASC = Sort.Direction.ASC;

  private static final Sort.Direction DESC = Sort.Direction.DESC;

  static SortRequest.Parameter param(String name, Sort.Direction dir) {
    return SortRequest.Parameter.builder().parameterName(name).direction(dir).build();
  }

  @Test
  void forCsv() {
    assertThat(SortRequest.forCsv(" , ")).isEqualTo(SortRequest.builder().build());
    assertThat(SortRequest.forCsv("foo, -bar , --fizz, foo"))
        .isEqualTo(
            SortRequest.builder()
                .sorting(
                    List.of(
                        param("foo", ASC),
                        param("bar", DESC),
                        param("-fizz", DESC),
                        param("foo", ASC)))
                .build());
  }
}
