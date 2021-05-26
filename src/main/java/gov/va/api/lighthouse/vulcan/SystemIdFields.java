package gov.va.api.lighthouse.vulcan;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import org.springframework.data.jpa.domain.Specification;

/** Provides Specification function shortcuts for simple system to field name mappings. */
@Data
@NoArgsConstructor
public class SystemIdFields<EntityT> {
  private final List<SystemFieldMapping<EntityT>> fields = new ArrayList<>();
  private String parameterName;

  public static <E> SystemIdFields<E> forEntity(Class<E> entity) {
    return new SystemIdFields<>();
  }

  public SystemIdFields<EntityT> add(String system, String fieldName) {
    fields.add(SystemFieldMapping.<EntityT>of(system, fieldName));
    return this;
  }

  public SystemIdFields<EntityT> add(
      String system, String fieldName, Function<String, String> converter) {
    fields.add(SystemFieldMapping.<EntityT>of(system, fieldName, converter));
    return this;
  }

  public SystemIdFields<EntityT> addWithCustomSystemAndCodeHandler(
      String system,
      String fieldName,
      BiFunction<String, String, Specification<EntityT>> function) {
    fields.add(SystemFieldMapping.<EntityT>of(system, fieldName, function));
    return this;
  }

  /** Generates BiFunction from mappings. */
  public BiFunction<String, String, Specification<EntityT>> matchSystemAndCode() {
    return (system, code) -> {
      for (SystemFieldMapping<EntityT> field : fields) {
        if (field.system().equals(system)) {
          return field.withSystemAndCode().apply(system, code);
        }
      }
      return (root, criteriaQuery, criteriaBuilder) -> {
        return criteriaBuilder.disjunction();
      };
    };
  }

  /** Generates Function from mappings. */
  public Function<String, Specification<EntityT>> matchSystemOnly() {
    return system -> {
      for (SystemFieldMapping<EntityT> field : fields) {
        if (field.system().equals(system)) {
          return field.withSystem().apply(system);
        }
      }
      return (root, criteriaQuery, criteriaBuilder) -> {
        return criteriaBuilder.disjunction();
      };
    };
  }

  @Builder
  @Value
  public static class SystemFieldMapping<EntityT> {
    @NonNull String system;

    @NonNull String fieldName;

    @NonNull Function<String, Specification<EntityT>> withSystem;

    @NonNull BiFunction<String, String, Specification<EntityT>> withSystemAndCode;

    /** Creates mapping for system and field name. */
    public static <E> SystemFieldMapping<E> of(String system, String fieldName) {
      return of(system, fieldName, Function.identity());
    }

    /** Creates mapping for system and field name with value converter. */
    public static <E> SystemFieldMapping<E> of(
        String system, String fieldName, Function<String, String> converter) {
      return SystemFieldMapping.<E>builder()
          .system(system)
          .fieldName(fieldName)
          .withSystem(
              (s) -> {
                return Specifications.<E>selectNotNull(fieldName);
              })
          .withSystemAndCode(
              (s, c) -> {
                return Specifications.<E>select(fieldName, converter.apply(c));
              })
          .build();
    }

    /** Creates mapping with a custom function. */
    public static <E> SystemFieldMapping<E> of(
        String system,
        String fieldName,
        BiFunction<String, String, Specification<E>> customSystemAndCodeFunction) {
      return SystemFieldMapping.<E>builder()
          .system(system)
          .fieldName(fieldName)
          .withSystem(
              (s) -> {
                return Specifications.<E>selectNotNull(fieldName);
              })
          .withSystemAndCode(customSystemAndCodeFunction)
          .build();
    }
  }
}
