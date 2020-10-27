package gov.va.api.lighthouse.vulcan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gov.va.api.lighthouse.vulcan.fugazi.FugaziEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

class SpecificationsTest {

  @SuppressWarnings("unchecked")
  @Test
  void collectAll() {
    assertThat(Stream.<Specification<FugaziEntity>>of().collect(Specifications.all())).isNull();

    List<Specification<FugaziEntity>> specs = new ArrayList<>(3);
    specs.add(mock(Specification.class));
    specs.add(mock(Specification.class));
    specs.add(mock(Specification.class));
    List<Specification<FugaziEntity>> anded = new ArrayList<>();
    specs.forEach(
        m -> {
          when(m.and(any(Specification.class)))
              .thenAnswer(
                  i -> {
                    anded.add(i.getArgument(0));
                    return i.getMock();
                  });
        });

    Specification<FugaziEntity> all = specs.stream().collect(Specifications.all());
    // all will be one of the specs passed in
    assertThat(specs.remove(all)).isTrue();
    // the other two specs will have been anded with it
    assertThat(anded).containsExactlyInAnyOrderElementsOf(specs);
    verify(all).and(specs.get(1));
    verify(all).and(specs.get(0));
  }

  @SuppressWarnings("unchecked")
  @Test
  void collectAny() {
    assertThat(Stream.<Specification<FugaziEntity>>of().collect(Specifications.any())).isNull();

    List<Specification<FugaziEntity>> specs = new ArrayList<>(3);
    specs.add(mock(Specification.class));
    specs.add(mock(Specification.class));
    specs.add(mock(Specification.class));
    List<Specification<FugaziEntity>> ored = new ArrayList<>();
    specs.forEach(
        m -> {
          when(m.or(any(Specification.class)))
              .thenAnswer(
                  i -> {
                    ored.add(i.getArgument(0));
                    return i.getMock();
                  });
        });

    Specification<FugaziEntity> all = specs.stream().collect(Specifications.any());
    // all will be one of the specs passed in
    assertThat(specs.remove(all)).isTrue();
    // the other two specs will have been anded with it
    assertThat(ored).containsExactlyInAnyOrderElementsOf(specs);
    verify(all).or(specs.get(1));
    verify(all).or(specs.get(0));
  }
}
