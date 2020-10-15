package gov.va.api.lighthouse.vulcan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.va.api.health.autoconfig.configuration.JacksonConfig;
import gov.va.api.lighthouse.vulcan.fugazi.FugaziApplication;
import gov.va.api.lighthouse.vulcan.fugazi.FugaziDto;
import gov.va.api.lighthouse.vulcan.fugazi.FugaziDto.Food;
import gov.va.api.lighthouse.vulcan.fugazi.FugaziEntity;
import gov.va.api.lighthouse.vulcan.fugazi.FugaziRepository;
import java.time.Instant;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@SpringJUnitConfig(FugaziApplication.class)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@TestPropertySource(
    properties = {
      "spring.main.banner-mode=off",
      "ssl.use-trust-store=false",
      "server.ssl.client-auth=none",
      "server.ssl.enabled=false",
      "ssl.enable-client=false"
    })
@Slf4j
@Transactional
class VulcanTest {
  @Autowired MockMvc mvc;

  @Autowired FugaziRepository repo;

  ObjectMapper mapper = JacksonConfig.createMapper();

  private FugaziDto nachos2005;

  private FugaziDto moreNachos2005;

  private FugaziDto tacos2005;

  private FugaziDto tacos2006;

  @Test
  void csvListMapping() {
    assertThat(req("/fugazi?food=")).isEmpty();
    assertThat(req("/fugazi?food=,")).isEmpty();
    assertThat(req("/fugazi?food=NOPE")).isEmpty();
    assertThat(req("/fugazi?food=NACHOS")).containsExactly(nachos2005);
    assertThat(req("/fugazi?food=NACHOS,NACHOS")).containsExactly(nachos2005);
    assertThat(req("/fugazi?food=NACHOS,TACOS"))
        .containsExactlyInAnyOrder(nachos2005, tacos2005, tacos2006);
    assertThat(req("/fugazi?food=NACHOS,,TACOS,TACOS"))
        .containsExactlyInAnyOrder(nachos2005, tacos2005, tacos2006);
    assertThat(req("/fugazi?food=NACHOS,NOPE")).containsExactly(nachos2005);

    assertThat(req("/fugazi?xfood=NACHOS")).containsExactly(nachos2005);
  }

  @BeforeEach
  void insertData() {
    nachos2005 = save("nachos2005", "2005-01-21T07:57:00Z", Food.NACHOS);
    moreNachos2005 = save("moreNachos2005", "2005-01-21T07:57:00Z", Food.EVEN_MORE_NACHOS);
    tacos2005 = save("tacos2005", "2005-01-21T07:57:00Z", Food.TACOS);
    tacos2006 = save("tacos2006", "2006-01-21T07:57:00Z", Food.TACOS);
  }

  @Test
  void mulitpleParametersAreAnded() {
    assertThat(req("/fugazi?food=NACHOS,TACOS&name:contains=nacho")).containsExactly(nachos2005);
  }

  @SneakyThrows
  List<FugaziDto> req(String uri) {
    var json =
        mvc.perform(get(uri))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return mapper.readValue(json, new TypeReference<>() {});
  }

  @SneakyThrows
  FugaziDto save(String name, String date, Food food) {
    var dto = FugaziDto.builder().name(name).dinner(food).favoriteNumber(name.length()).build();
    Instant time = Instant.parse(date);
    repo.save(
        FugaziEntity.builder()
            .name(name)
            .food(food.toString())
            .date(time)
            .millis(time.toEpochMilli())
            .payload(mapper.writeValueAsString(dto))
            .build());
    return dto;
  }

  @Test
  void stringMapping() {
    assertThat(req("/fugazi?name=")).isEmpty();
    assertThat(req("/fugazi?name=nachos2005")).containsExactly(nachos2005);
    assertThat(req("/fugazi?name=tacos")).containsExactlyInAnyOrder(tacos2005, tacos2006);
    assertThat(req("/fugazi?name:exact=nachos")).isEmpty();
    assertThat(req("/fugazi?name:exact=")).isEmpty();
    assertThat(req("/fugazi?name:contains=nachos"))
        .containsExactlyInAnyOrder(nachos2005, moreNachos2005);
    assertThat(req("/fugazi?name:contains=")).isEmpty();

    assertThat(req("/fugazi?xname=nachos2005")).containsExactly(nachos2005);
  }

  @Test
  void valueMapping() {
    assertThat(req("/fugazi?millis=")).isEmpty();
    assertThat(req("/fugazi?millis=2006-01-21T07:57:00Z")).containsExactly(tacos2006);

    assertThat(req("/fugazi?xmillis=2006-01-21T07:57:00Z")).containsExactly(tacos2006);
  }
}
