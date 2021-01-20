package gov.va.api.lighthouse.vulcan.fugazi;

import static gov.va.api.lighthouse.vulcan.Vulcan.useUrl;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.va.api.health.autoconfig.configuration.JacksonConfig;
import gov.va.api.lighthouse.vulcan.Vulcan;
import gov.va.api.lighthouse.vulcan.VulcanConfiguration;
import gov.va.api.lighthouse.vulcan.VulcanConfiguration.PagingConfiguration;
import gov.va.api.lighthouse.vulcan.fugazi.FugaziDto.Food;
import gov.va.api.lighthouse.vulcan.mappings.Mappings;
import gov.va.api.lighthouse.vulcan.mappings.TokenParameter;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
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
                .sort(Sort.by("id").ascending())
                .baseUrlStrategy(useUrl("http://vulcan.com"))
                .build())
        .mappings(
            Mappings.forEntity(FugaziEntity.class)
                .string("name")
                .string("xname", "name")
                .value("namevalue", "name")
                .csvList("food")
                .csvList("xfood", "food")
                .value("namevalue", "name")
                .value("millis", v -> Instant.parse(v).toEpochMilli())
                .value("xmillis", "millis", v -> Instant.parse(v).toEpochMilli())
                .dateAsInstant("xdate", "date")
                .token("foodtoken", "food", this::foodIsSupported, this::foodValues)
                .tokenList("foodtokencsv", "food", this::foodIsSupported, this::foodValues)
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
        .entities()
        .map(this::asFoo)
        .collect(toList());
  }
}
