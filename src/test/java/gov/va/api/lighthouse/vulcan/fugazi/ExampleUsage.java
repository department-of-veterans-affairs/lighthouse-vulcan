package gov.va.api.lighthouse.vulcan.fugazi;

import static gov.va.api.lighthouse.vulcan.Rules.atLeastOneParameterOf;
import static gov.va.api.lighthouse.vulcan.Rules.forbiddenParameters;
import static gov.va.api.lighthouse.vulcan.Rules.ifParameter;
import static gov.va.api.lighthouse.vulcan.Rules.parametersAlwaysSpecifiedTogether;
import static gov.va.api.lighthouse.vulcan.Rules.parametersNeverSpecifiedTogether;
import static gov.va.api.lighthouse.vulcan.Vulcan.rejectRequest;
import static gov.va.api.lighthouse.vulcan.Vulcan.useUrl;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.va.api.health.autoconfig.configuration.JacksonConfig;
import gov.va.api.lighthouse.vulcan.SortRequest;
import gov.va.api.lighthouse.vulcan.Vulcan;
import gov.va.api.lighthouse.vulcan.VulcanConfiguration;
import gov.va.api.lighthouse.vulcan.VulcanConfiguration.PagingConfiguration;
import gov.va.api.lighthouse.vulcan.mappings.Mappings;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

public class ExampleUsage {
  final ObjectMapper mapper = JacksonConfig.createMapper();

  FugaziRepository repo;

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
                .sortDefault(Sort.by("id").ascending())
                .sortableParameters(this::sortableParameters)
                .baseUrlStrategy(useUrl("http://vulcan.com"))
                .build())
        .mappings(
            Mappings.forEntity(FugaziEntity.class)
                .string("name")
                .value("millis", v -> Instant.parse(v).toEpochMilli())
                .dateAsInstant("when", "date")
                .dateAsLongMilliseconds("whenLong", "millis")
                .reference(
                    "foodref",
                    "food",
                    Set.of("mexican, italian"),
                    "mexican",
                    rp -> rp.type().equals("mexican"),
                    rp -> rp.publicId())
                .get())
        .defaultQuery(rejectRequest())
        .rules(
            List.of(
                atLeastOneParameterOf("patient", "_id"),
                parametersNeverSpecifiedTogether("patient", "_id"),
                forbiddenParameters("client-key"),
                ifParameter("patient").thenAlsoAtLeastOneParameterOf("category", "code"),
                ifParameter("category").thenForbidParameters("code"),
                parametersAlwaysSpecifiedTogether("latitude", "longitude")))
        .build();
  }

  @GetMapping
  public ResponseEntity<List<FugaziDto>> get(HttpServletRequest request) {
    // Invoke Vulcan to perform determine and perform the approriate query
    var result = Vulcan.forRepo(repo).config(configuration()).build().search(request);

    // Process the entities anyway you want. Here we'll map them a DTO.
    var body = result.entities().map(this::asFoo).collect(toList());
    var response = ResponseEntity.ok(body);

    // Enhance your response with paging information anyway you want.
    // I'll just add some headers, but you could do whatever.
    var headers = response.getHeaders();
    result.paging().firstPageUrl().ifPresent(url -> headers.add("X-FIRST-PAGE", url));
    result.paging().previousPageUrl().ifPresent(url -> headers.add("X-PREVIOUS-PAGE", url));
    result.paging().thisPageUrl().ifPresent(url -> headers.add("X-THIS-PAGE", url));
    result.paging().nextPageUrl().ifPresent(url -> headers.add("X-NEXT-PAGE", url));
    result.paging().lastPageUrl().ifPresent(url -> headers.add("X-LAST-PAGE", url));
    return response;
  }

  private Sort sortableParameters(SortRequest req) {
    return req.sorting().stream()
        .filter(p -> p.parameterName().equals("when"))
        .findFirst()
        .map(p -> Sort.by(p.direction(), "date").and(Sort.by("id").ascending()))
        .orElse(null);
  }
}
