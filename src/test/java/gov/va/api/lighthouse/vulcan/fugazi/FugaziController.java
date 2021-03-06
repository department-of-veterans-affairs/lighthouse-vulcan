package gov.va.api.lighthouse.vulcan.fugazi;

import static gov.va.api.lighthouse.vulcan.Specifications.strings;
import static gov.va.api.lighthouse.vulcan.Vulcan.useUrl;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.va.api.health.autoconfig.configuration.JacksonConfig;
import gov.va.api.lighthouse.vulcan.InvalidRequest;
import gov.va.api.lighthouse.vulcan.SortRequest;
import gov.va.api.lighthouse.vulcan.Specifications;
import gov.va.api.lighthouse.vulcan.SystemIdFields;
import gov.va.api.lighthouse.vulcan.Vulcan;
import gov.va.api.lighthouse.vulcan.VulcanConfiguration;
import gov.va.api.lighthouse.vulcan.VulcanConfiguration.PagingConfiguration;
import gov.va.api.lighthouse.vulcan.VulcanResult;
import gov.va.api.lighthouse.vulcan.fugazi.FugaziDto.Food;
import gov.va.api.lighthouse.vulcan.mappings.Mappings;
import gov.va.api.lighthouse.vulcan.mappings.ReferenceParameter;
import gov.va.api.lighthouse.vulcan.mappings.TokenParameter;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping(
    value = {"/fugazi"},
    produces = {"application/json"})
public class FugaziController {
  final ObjectMapper mapper = JacksonConfig.createMapper();

  @Autowired FugaziRepository repo;

  @SneakyThrows
  private FugaziDto asFoo(FugaziEntity e) {
    return mapper.readValue(e.payload(), FugaziDto.class);
  }

  private VulcanConfiguration<FugaziEntity> configuration() {
    return VulcanConfiguration.forEntity(FugaziEntity.class)
        .paging(
            PagingConfiguration.builder()
                .pageParameter("page")
                .countParameter("count")
                .defaultCount(30)
                .maxCount(100)
                .baseUrlStrategy(useUrl("http://vulcan.com"))
                .sortDefault(Sort.by("id").ascending())
                .sortableParameters(this::sortableParameters)
                .build())
        .mappings(
            Mappings.forEntity(FugaziEntity.class)
                .string("name")
                .string("xname", "name")
                .string("foodOrBase", p -> (Set.of("food", "base")))
                .value("namevalue", "name")
                .value("namevalue", "name")
                .value("millis", v -> Instant.parse(v).toEpochMilli())
                .value("xmillis", "millis", v -> Instant.parse(v).toEpochMilli())
                .values("nameAndFood", this::nameFoodValues)
                .dateAsInstant("xdate", "date")
                .dateAsLongMilliseconds("ydate", "millis")
                .tokens("foodSpecToken", this::foodIsSupported, this::foodSpecification)
                .tokens("foodSpecNullable", this::foodIsSupported, this::foodSpecificationNullable)
                .tokens(
                    "foodSpecHelper",
                    this::foodIsSupportedAnyExplicitSystem,
                    this::foodSpecificationHelper)
                .reference(
                    "foodref",
                    "name",
                    Set.of("mexican", "italian"),
                    "mexican",
                    this::foodReferenceIsSupported,
                    this::foodReferenceValues)
                .get())
        .defaultQuery(Vulcan.returnNothing())
        .build();
  }

  @SuppressWarnings("RedundantIfStatement")
  private boolean foodIsSupported(TokenParameter token) {
    if (token.isSystemExplicitAndUnsupported("http://food")
        || token.isCodeExplicitAndUnsupported(Food.values())
        || token.hasExplicitlyNoSystem()) {
      return false;
    }
    return true;
  }

  private boolean foodIsSupportedAnyExplicitSystem(TokenParameter token) {
    return token.hasExplicitSystem() || token.hasExplicitCode();
  }

  private boolean foodReferenceIsSupported(ReferenceParameter referenceParameter) {
    log.info(
        "Name: {}, Type: {}, Value: {}, ID: {}, URL: {}",
        referenceParameter.parameterName(),
        referenceParameter.type(),
        referenceParameter.value(),
        referenceParameter.publicId(),
        referenceParameter.url().isPresent() ? referenceParameter.url().get() : "no url");
    var isSafeUrl = true;
    if (referenceParameter.url().isPresent()) {
      isSafeUrl =
          referenceParameter
              .url()
              .get()
              .equals("https://goodfood.com/mexican/" + referenceParameter.publicId());
    }
    return (StringUtils.equals("mexican", referenceParameter.type())
            || StringUtils.equals("italian", referenceParameter.type()))
        && isSafeUrl;
  }

  private String foodReferenceValues(ReferenceParameter referenceParameter) {
    return referenceParameter.publicId();
  }

  private Specification<FugaziEntity> foodSpecification(TokenParameter token) {
    return token
        .behavior()
        .onExplicitSystemAndExplicitCode((s, c) -> Specifications.<FugaziEntity>select("food", c))
        .onAnySystemAndExplicitCode(c -> Specifications.<FugaziEntity>select("food", c))
        .onNoSystemAndExplicitCode(c -> Specifications.<FugaziEntity>select("food", c))
        .onExplicitSystemAndAnyCode(
            s -> Specifications.<FugaziEntity>selectInList("food", strings(Food.class)))
        .build()
        .execute();
  }

  private Specification<FugaziEntity> foodSpecificationHelper(TokenParameter token) {
    var helper =
        SystemIdFields.forEntity(FugaziEntity.class)
            .parameterName("foodSpecHelper")
            .add("http://food", "food")
            .add("http://food-with-prefix", "food", this::removeFoodPrefix)
            .addWithCustomSystemAndCodeHandler(
                "http://food-custom",
                "food",
                (system, code) -> {
                  return Specifications.<FugaziEntity>select("food", code);
                });
    return token
        .behavior()
        .onExplicitSystemAndExplicitCode(helper.matchSystemAndCode())
        .onAnySystemAndExplicitCode(c -> Specifications.<FugaziEntity>select("food", c))
        .onNoSystemAndExplicitCode(c -> Specifications.<FugaziEntity>select("food", c))
        .onExplicitSystemAndAnyCode(helper.matchSystemOnly())
        .build()
        .execute();
  }

  private Specification<FugaziEntity> foodSpecificationNullable(TokenParameter token) {
    return token
        .behavior()
        .onExplicitSystemAndExplicitCode((s, c) -> Specifications.<FugaziEntity>select("food", c))
        .onAnySystemAndExplicitCode(c -> Specifications.<FugaziEntity>select("food", c))
        .onNoSystemAndExplicitCode(c -> Specifications.<FugaziEntity>select("food", c))
        .onExplicitSystemAndAnyCode(s -> Specifications.<FugaziEntity>selectNotNull("food"))
        .build()
        .execute();
  }

  private Collection<String> foodValues(TokenParameter token) {
    return token
        .behavior()
        .onExplicitSystemAndExplicitCode((s, c) -> foodsForStrings(c))
        .onAnySystemAndExplicitCode(this::foodsForStrings)
        .onNoSystemAndExplicitCode(this::foodsForStrings)
        .onExplicitSystemAndAnyCode(s -> Set.of(Food.values()))
        .build()
        .execute()
        .stream()
        .filter(Objects::nonNull)
        .map(Food::toString)
        .collect(toList());
  }

  Set<Food> foodsForStrings(String s) {
    if (isBlank(s)) {
      return null;
    }
    return Set.of(Food.valueOf(s));
  }

  @GetMapping
  public List<FugaziDto> get(HttpServletRequest request) {
    log.info("URL {}", request.getRequestURL());
    log.info("Query {}", request.getQueryString());
    return Vulcan.forRepo(repo)
        .config(configuration())
        .build()
        .search(request)
        .map(VulcanResult::entities)
        .map(this::asFoo)
        .collect(toList());
  }

  private Map<String, ?> nameFoodValues(String value) {
    var parts = value.split(":", -1);
    if (parts.length != 2) {
      throw InvalidRequest.badParameter("namefood", value, "format is name:food");
    }
    return Map.of("name", parts[0], "food", parts[1]);
  }

  private String removeFoodPrefix(String value) {
    if (value == null) return null;
    if (StringUtils.startsWith(value, "food_")) {
      return StringUtils.removeStart(value, "food_");
    }
    return value;
  }

  private Sort sortableParameters(SortRequest req) {
    return req.sorting().stream()
        .filter(p -> p.parameterName().equals("xname"))
        .findFirst()
        .map(p -> Sort.by(p.direction(), "name").and(Sort.by("id").ascending()))
        .orElse(null);
  }
}
