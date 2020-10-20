package gov.va.api.lighthouse.vulcan.fugazi;

import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.va.api.health.autoconfig.configuration.JacksonConfig;
import gov.va.api.lighthouse.vulcan.Mappings;
import gov.va.api.lighthouse.vulcan.Vulcan;
import gov.va.api.lighthouse.vulcan.Vulcan.PagingParameters;
import gov.va.api.lighthouse.vulcan.VulcanConfiguration;
import java.time.Instant;
import java.util.List;
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
            PagingParameters.builder()
                .pageParameter("page")
                .countParameter("_count")
                .defaultCount(30)
                .maxCount(100)
                .sort(Sort.by("id").ascending())
                .build())
        .mappings(
            Mappings.forEntity(FugaziEntity.class)
                .string("name")
                .string("xname", "name")
                .csvList("food")
                .csvList("xfood", "food")
                .value("millis", v -> Instant.parse(v).toEpochMilli())
                .value("xmillis", "millis", v -> Instant.parse(v).toEpochMilli())
                .dateAsInstant("xdate", "date")
                .get())
        .build();
  }

  @GetMapping
  public List<FugaziDto> get(HttpServletRequest request) {
    return Vulcan.forRepo(repo).config(configuration()).build().forge(request).stream()
        .map(this::asFoo)
        .collect(toList());
  }
}
