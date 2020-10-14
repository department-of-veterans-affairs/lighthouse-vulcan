package gov.va.api.lighthouse.vulcan.fugazi;

import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.va.api.health.autoconfig.configuration.JacksonConfig;
import gov.va.api.lighthouse.vulcan.Mappings;
import gov.va.api.lighthouse.vulcan.Vulcan;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

  @GetMapping
  public List<FugaziDto> get(HttpServletRequest request) {
    return Vulcan.forRepo(repo)
        .mappings(
            Mappings.forEntity(FugaziEntity.class)
                .string("name")
                .string("xname", "name")
                .csvList("food")
                .csvList("xfood", "food")
                .instant("date")
                .instant("xdate", "date")
                .get())
        .build()
        .forge(request)
        .stream()
        .map(this::asFoo)
        .collect(toList());
  }
}
