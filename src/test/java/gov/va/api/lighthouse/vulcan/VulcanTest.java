package gov.va.api.lighthouse.vulcan;

import static gov.va.api.lighthouse.vulcan.Vulcan.returnNothing;
import static gov.va.api.lighthouse.vulcan.Vulcan.useRequestUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.va.api.health.autoconfig.configuration.JacksonConfig;
import gov.va.api.lighthouse.vulcan.VulcanConfiguration.PagingConfiguration;
import gov.va.api.lighthouse.vulcan.VulcanResult.Paging;
import gov.va.api.lighthouse.vulcan.fugazi.FugaziApplication;
import gov.va.api.lighthouse.vulcan.fugazi.FugaziDto;
import gov.va.api.lighthouse.vulcan.fugazi.FugaziDto.Base;
import gov.va.api.lighthouse.vulcan.fugazi.FugaziDto.Food;
import gov.va.api.lighthouse.vulcan.fugazi.FugaziEntity;
import gov.va.api.lighthouse.vulcan.fugazi.FugaziRepository;
import gov.va.api.lighthouse.vulcan.mappings.Mappings;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockHttpServletRequest;
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

  private FugaziDto tacos2007;

  private FugaziDto tacos2008;

  private FugaziDto unknown;

  @SuppressWarnings("unused")
  static Stream<Arguments> pageAndCount() {
    /*
    String requestName,
    String requestPage,
    String requestCount,
    int totalRecords,
    int totalPages,
    Integer firstPage,
    Integer previousPage,
    Integer thisPage,
    Integer nextPage,
    Integer lastPage
    */

    return Stream.of(
        arguments("a", "1", "10", 6, 1, 1, null, 1, null, 1),
        arguments("a", "2", "10", 6, 1, 1, null, 2, null, 1),
        arguments("a", "1", "2", 6, 3, 1, null, 1, 2, 3),
        arguments("a", "2", "2", 6, 3, 1, 1, 2, 3, 3),
        // no records found
        arguments("a", "3", "2", 6, 3, 1, 2, 3, null, 3),
        // count only results
        arguments("a", "2", "5", 6, 2, 1, 1, 2, null, 2),
        arguments("nope", "1", "5", 0, 0, null, null, 1, null, null),
        arguments("a", "1", "0", 6, 0, null, null, 1, null, null));
  }

  @BeforeEach
  void _insertData() {
    unknown = _save("unknown", "2004-01-23T04:04:00Z", null, null);
    nachos2005 = _save("nachos2005", "2005-01-21T07:57:00Z", Food.NACHOS, Base.CHIPS);
    moreNachos2005 =
        _save("moreNachos2005", "2005-01-22T07:57:00Z", Food.EVEN_MORE_NACHOS, Base.CHIPS);
    tacos2005 = _save("tacos2005", "2005-01-23T07:57:00Z", Food.TACOS, Base.TORTILLAS);
    tacos2006 = _save("tacos2006", "2006-01-21T07:57:00Z", Food.TACOS, Base.TORTILLAS);
    tacos2007 = _save("tacos2007", "2007-01-21T07:57:00Z", Food.TACOS, Base.TORTILLAS);
    tacos2008 = _save("tacos2008", "2008-01-21T07:57:00Z", Food.TACOS, Base.TORTILLAS);
  }

  @SneakyThrows
  FugaziDto _save(String name, String date, Food food, Base base) {
    var dto = FugaziDto.builder().name(name).dinner(food).favoriteNumber(name.length()).build();
    Instant time = Instant.parse(date);
    repo.save(
        FugaziEntity.builder()
            .name(name)
            .food(food == null ? null : food.toString())
            .base(base == null ? null : base.toString())
            .date(time)
            .millis(time.toEpochMilli())
            .payload(mapper.writeValueAsString(dto))
            .build());
    return dto;
  }

  @SneakyThrows
  String badReq(String uri) {
    return mvc.perform(get(uri))
        .andExpect(status().isBadRequest())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  @Test
  void compositeValuesAreCombinedWithAnd() {
    assertThat(req("/fugazi?nameAndFood=nachos2005:NACHOS")).containsExactly(nachos2005);
    assertThat(req("/fugazi?nameAndFood=nachos2005:TACOS")).isEmpty();
    assertThat(req("/fugazi?nameAndFood=tacos2005:TACOS")).containsExactly(tacos2005);
    assertThat(badReq("/fugazi?nameAndFood=tacos2005+NOPE+TACOS")).isEmpty();
  }

  @Test
  void defaultQueryCausesEmptyResult() {
    var vulcan =
        Vulcan.forRepo(repo)
            .config(
                VulcanConfiguration.forEntity(FugaziEntity.class)
                    .paging(
                        PagingConfiguration.builder()
                            .pageParameter("page")
                            .countParameter("count")
                            .defaultCount(3)
                            .maxCount(10)
                            .sortDefault(Sort.by("id").ascending())
                            .baseUrlStrategy(useRequestUrl())
                            .build())
                    .mappings(Mappings.forEntity(FugaziEntity.class).string("name").get())
                    .defaultQuery(Vulcan.returnNothing())
                    .build())
            .build();
    var request = new MockHttpServletRequest();
    request.setRequestURI("/fugazi");
    var result = vulcan.search(request);
    assertThat(result.paging().totalRecords()).isEqualTo(0);
  }

  @Test
  void defaultQueryCausesInvalidParametersException() {
    var vulcan =
        Vulcan.forRepo(repo)
            .config(
                VulcanConfiguration.forEntity(FugaziEntity.class)
                    .paging(
                        PagingConfiguration.builder()
                            .pageParameter("page")
                            .countParameter("count")
                            .defaultCount(3)
                            .maxCount(10)
                            .sortDefault(Sort.by("id").ascending())
                            .baseUrlStrategy(useRequestUrl())
                            .build())
                    .mappings(Mappings.forEntity(FugaziEntity.class).string("name").get())
                    .defaultQuery(Vulcan.rejectRequest())
                    .build())
            .build();
    var request = new MockHttpServletRequest();
    request.setRequestURI("/fugazi");
    assertThatExceptionOfType(InvalidRequest.class).isThrownBy(() -> vulcan.search(request));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"?foodSpecToken=|", "?foodSpecToken=NACHOS,|", "?xdate=nope", "?xdate=no2006"})
  @SneakyThrows
  void invalidParameterSearchs(String query) {
    mvc.perform(get("/fugazi" + query)).andExpect(status().isBadRequest());
  }

  @SuppressWarnings("SpellCheckingInspection")
  @Test
  void mappingDateAsInstant() {
    assertThat(req("/fugazi?xdate=")).isEmpty();
    assertThat(req("/fugazi?xdate=2006-01-21")).containsExactly(tacos2006);
    assertThat(req("/fugazi?xdate=eq2006-01-21")).containsExactly(tacos2006);
    assertThat(req("/fugazi?xdate=gt2006-01-20")).containsExactly(tacos2006, tacos2007, tacos2008);
    assertThat(req("/fugazi?xdate=sa2006-01-20")).containsExactly(tacos2006, tacos2007, tacos2008);
    assertThat(req("/fugazi?xdate=ge2005-01-22"))
        .containsExactlyInAnyOrder(tacos2005, moreNachos2005, tacos2006, tacos2007, tacos2008);
    assertThat(req("/fugazi?xdate=lt2005-01-22")).containsExactlyInAnyOrder(unknown, nachos2005);
    assertThat(req("/fugazi?xdate=eb2005-01-22")).containsExactlyInAnyOrder(unknown, nachos2005);
    assertThat(req("/fugazi?xdate=le2005-01-22"))
        .containsExactlyInAnyOrder(unknown, nachos2005, moreNachos2005);
    assertThat(req("/fugazi?xdate=ap2005-01-22"))
        .containsExactlyInAnyOrder(nachos2005, moreNachos2005, tacos2005);
    assertThat(req("/fugazi?xdate=ne2006"))
        .containsExactlyInAnyOrder(
            unknown, nachos2005, moreNachos2005, tacos2005, tacos2007, tacos2008);
    assertThat(req("/fugazi?xdate=gt2005-01-20&xdate=lt2005-02"))
        .containsExactlyInAnyOrder(nachos2005, tacos2005, moreNachos2005);
  }

  @SuppressWarnings("SpellCheckingInspection")
  @Test
  void mappingDateAsLongMilliseconds() {
    assertThat(req("/fugazi?ydate=")).isEmpty();
    assertThat(req("/fugazi?ydate=2006-01-21")).containsExactly(tacos2006);
    assertThat(req("/fugazi?ydate=eq2006-01-21")).containsExactly(tacos2006);
    assertThat(req("/fugazi?ydate=gt2006-01-20")).containsExactly(tacos2006, tacos2007, tacos2008);
    assertThat(req("/fugazi?ydate=sa2006-01-20")).containsExactly(tacos2006, tacos2007, tacos2008);
    assertThat(req("/fugazi?ydate=ge2005-01-22"))
        .containsExactlyInAnyOrder(tacos2005, moreNachos2005, tacos2006, tacos2007, tacos2008);
    assertThat(req("/fugazi?ydate=lt2005-01-22")).containsExactlyInAnyOrder(unknown, nachos2005);
    assertThat(req("/fugazi?ydate=eb2005-01-22")).containsExactlyInAnyOrder(unknown, nachos2005);
    assertThat(req("/fugazi?ydate=le2005-01-22"))
        .containsExactlyInAnyOrder(unknown, nachos2005, moreNachos2005);
    assertThat(req("/fugazi?ydate=ap2005-01-22"))
        .containsExactlyInAnyOrder(nachos2005, moreNachos2005, tacos2005);
    assertThat(req("/fugazi?ydate=ne2006"))
        .containsExactlyInAnyOrder(
            unknown, nachos2005, moreNachos2005, tacos2005, tacos2007, tacos2008);
    assertThat(req("/fugazi?ydate=gt2005-01-20&ydate=lt2005-02"))
        .containsExactlyInAnyOrder(nachos2005, tacos2005, moreNachos2005);
  }

  @Test
  void mappingReference() {
    assertThat(req("/fugazi?foodref=")).isEmpty();
    assertThat(req("/fugazi?foodref:mexican=nachos2005")).containsExactly(nachos2005);
    assertThat(req("/fugazi?foodref:italian=tacos2005")).containsExactly(tacos2005);
    assertThat(req("/fugazi?foodref=mexican/nachos2005")).containsExactly(nachos2005);
    assertThat(req("/fugazi?foodref=https://goodfood.com/mexican/nachos2005"))
        .containsExactly(nachos2005);
  }

  @SuppressWarnings("SpellCheckingInspection")
  @Test
  void mappingString() {
    assertThat(req("/fugazi?name=")).isEmpty();
    assertThat(req("/fugazi?name=nachos2005")).containsExactly(nachos2005);
    assertThat(req("/fugazi?name=tacos"))
        .containsExactlyInAnyOrder(tacos2005, tacos2006, tacos2007, tacos2008);
    assertThat(req("/fugazi?name:exact=nachos")).isEmpty();
    assertThat(req("/fugazi?name:exact=")).isEmpty();
    assertThat(req("/fugazi?name:contains=acho"))
        .containsExactlyInAnyOrder(nachos2005, moreNachos2005);
    assertThat(req("/fugazi?name:contains=")).isEmpty();
    assertThat(req("/fugazi?xname=nachos2005")).containsExactly(nachos2005);
  }

  @Test
  void mappingToken() {
    assertThat(req("/fugazi?foodSpecToken=")).isEmpty();
    assertThat(req("/fugazi?foodSpecToken=PIZZA")).isEmpty();
    assertThat(req("/fugazi?foodSpecToken=http://movie-theater|NACHOS")).isEmpty();
    assertThat(req("/fugazi?foodSpecToken=NACHOS")).containsExactly(nachos2005);
    assertThat(req("/fugazi?foodSpecToken=TACOS"))
        .containsExactlyInAnyOrder(tacos2005, tacos2006, tacos2007, tacos2008);
    assertThat(req("/fugazi?foodSpecToken=http://food|TACOS"))
        .containsExactlyInAnyOrder(tacos2005, tacos2006, tacos2007, tacos2008);
    assertThat(req("/fugazi?foodSpecToken=http://food|"))
        .containsExactlyInAnyOrder(
            nachos2005, moreNachos2005, tacos2005, tacos2006, tacos2007, tacos2008);

    assertThat(req("/fugazi?foodSpecNullable=")).isEmpty();
    assertThat(req("/fugazi?foodSpecNullable=PIZZA")).isEmpty();
    assertThat(req("/fugazi?foodSpecNullable=http://movie-theater|NACHOS")).isEmpty();
    assertThat(req("/fugazi?foodSpecNullable=NACHOS")).containsExactly(nachos2005);
    assertThat(req("/fugazi?foodSpecNullable=TACOS"))
        .containsExactlyInAnyOrder(tacos2005, tacos2006, tacos2007, tacos2008);
    assertThat(req("/fugazi?foodSpecNullable=http://food|PIZZA")).isEmpty();
    assertThat(req("/fugazi?foodSpecNullable=http://food|"))
        .containsExactlyInAnyOrder(
            nachos2005, moreNachos2005, tacos2005, tacos2006, tacos2007, tacos2008);

    assertThat(req("/fugazi?foodSpecHelper=")).isEmpty();
    assertThat(req("/fugazi?foodSpecHelper=PIZZA")).isEmpty();
    assertThat(req("/fugazi?foodSpecHelper=http://movie-theater|NACHOS")).isEmpty();
    assertThat(req("/fugazi?foodSpecHelper=http://movie-theater|")).isEmpty();
    assertThat(req("/fugazi?foodSpecHelper=NACHOS")).containsExactly(nachos2005);
    assertThat(req("/fugazi?foodSpecHelper=TACOS"))
        .containsExactlyInAnyOrder(tacos2005, tacos2006, tacos2007, tacos2008);
    assertThat(req("/fugazi?foodSpecHelper=http://food|PIZZA")).isEmpty();
    assertThat(req("/fugazi?foodSpecHelper=http://food|TACOS"))
        .containsExactlyInAnyOrder(tacos2005, tacos2006, tacos2007, tacos2008);
    assertThat(req("/fugazi?foodSpecHelper=http://food|"))
        .containsExactlyInAnyOrder(
            nachos2005, moreNachos2005, tacos2005, tacos2006, tacos2007, tacos2008);
    assertThat(req("/fugazi?foodSpecHelper=http://food-with-prefix|food_TACOS"))
        .containsExactlyInAnyOrder(tacos2005, tacos2006, tacos2007, tacos2008);
    assertThat(req("/fugazi?foodSpecHelper=http://food-custom|"))
        .containsExactlyInAnyOrder(
            nachos2005, moreNachos2005, tacos2005, tacos2006, tacos2007, tacos2008);
    assertThat(req("/fugazi?foodSpecHelper=http://food-custom|PIZZA")).isEmpty();
    assertThat(req("/fugazi?foodSpecHelper=http://food-custom|TACOS"))
        .containsExactlyInAnyOrder(tacos2005, tacos2006, tacos2007, tacos2008);

    assertThat(req("/fugazi?foodOrBase=NACHOS")).containsExactly(nachos2005);
    assertThat(req("/fugazi?foodOrBase=TORTILLAS"))
        .containsExactly(tacos2005, tacos2006, tacos2007, tacos2008);
    assertThat(req("/fugazi?foodOrBase=bread")).isEmpty();
    assertThat(req("/fugazi?foodOrBase=CHIPS")).containsExactly(nachos2005, moreNachos2005);
  }

  @Test
  void mappingTokenList() {
    assertThat(req("/fugazi?foodSpecToken=")).isEmpty();
    assertThat(req("/fugazi?foodSpecToken=,")).isEmpty();
    assertThat(req("/fugazi?foodSpecToken=PIZZA")).isEmpty();
    assertThat(req("/fugazi?foodSpecToken=http://movie-theater|NACHOS")).isEmpty();
    assertThat(req("/fugazi?foodSpecToken=NACHOS")).containsExactly(nachos2005);
    assertThat(req("/fugazi?foodSpecToken=TACOS"))
        .containsExactlyInAnyOrder(tacos2005, tacos2006, tacos2007, tacos2008);
    assertThat(req("/fugazi?foodSpecToken=http://food|TACOS"))
        .containsExactlyInAnyOrder(tacos2005, tacos2006, tacos2007, tacos2008);
    assertThat(req("/fugazi?foodSpecToken=http://food|TACOS,http://nope|NACHOS"))
        .containsExactlyInAnyOrder(tacos2005, tacos2006, tacos2007, tacos2008);
    assertThat(req("/fugazi?foodSpecToken=http://food|TACOS,http://food|NACHOS"))
        .containsExactlyInAnyOrder(tacos2005, tacos2006, tacos2007, tacos2008, nachos2005);
    assertThat(req("/fugazi?foodSpecToken=http://food|TACOS,NACHOS"))
        .containsExactlyInAnyOrder(tacos2005, tacos2006, tacos2007, tacos2008, nachos2005);
    assertThat(req("/fugazi?foodSpecToken=http://food|"))
        .containsExactlyInAnyOrder(
            nachos2005, moreNachos2005, tacos2005, tacos2006, tacos2007, tacos2008);

    assertThat(req("/fugazi?foodSpecHelper=")).isEmpty();
    assertThat(req("/fugazi?foodSpecHelper=,")).isEmpty();
    assertThat(req("/fugazi?foodSpecHelper=PIZZA")).isEmpty();
    assertThat(req("/fugazi?foodSpecHelper=http://movie-theater|NACHOS")).isEmpty();
    assertThat(req("/fugazi?foodSpecHelper=NACHOS")).containsExactly(nachos2005);
    assertThat(req("/fugazi?foodSpecHelper=TACOS"))
        .containsExactlyInAnyOrder(tacos2005, tacos2006, tacos2007, tacos2008);
    assertThat(req("/fugazi?foodSpecHelper=http://food|TACOS"))
        .containsExactlyInAnyOrder(tacos2005, tacos2006, tacos2007, tacos2008);
    assertThat(req("/fugazi?foodSpecHelper=http://food|TACOS,http://nope|NACHOS"))
        .containsExactlyInAnyOrder(tacos2005, tacos2006, tacos2007, tacos2008);
    assertThat(req("/fugazi?foodSpecHelper=http://food|TACOS,http://food|NACHOS"))
        .containsExactlyInAnyOrder(tacos2005, tacos2006, tacos2007, tacos2008, nachos2005);
    assertThat(req("/fugazi?foodSpecHelper=http://food|TACOS,NACHOS"))
        .containsExactlyInAnyOrder(tacos2005, tacos2006, tacos2007, tacos2008, nachos2005);
    assertThat(req("/fugazi?foodSpecHelper=http://food-with-prefix|food_TACOS,http://food|NACHOS"))
        .containsExactlyInAnyOrder(tacos2005, tacos2006, tacos2007, tacos2008, nachos2005);
    assertThat(req("/fugazi?foodSpecHelper=http://food|"))
        .containsExactlyInAnyOrder(
            nachos2005, moreNachos2005, tacos2005, tacos2006, tacos2007, tacos2008);
  }

  @SuppressWarnings("SpellCheckingInspection")
  @Test
  void mappingValue() {
    assertThat(req("/fugazi?millis=")).isEmpty();
    assertThat(req("/fugazi?millis=2006-01-21T07:57:00Z")).containsExactly(tacos2006);
    assertThat(req("/fugazi?xmillis=2006-01-21T07:57:00Z")).containsExactly(tacos2006);
  }

  @Test
  void multipleParametersAreCombinedWithAnd() {
    assertThat(req("/fugazi?foodSpecToken=NACHOS,TACOS&name:contains=nacho"))
        .containsExactly(nachos2005);
  }

  @ParameterizedTest
  @MethodSource
  void pageAndCount(
      String requestName,
      String requestPage,
      String requestCount,
      int totalRecords,
      int totalPages,
      Integer firstPage,
      Integer previousPage,
      Integer thisPage,
      Integer nextPage,
      Integer lastPage) {
    var vulcan =
        Vulcan.forRepo(repo)
            .config(
                VulcanConfiguration.forEntity(FugaziEntity.class)
                    .paging(
                        PagingConfiguration.builder()
                            .pageParameter("page")
                            .countParameter("count")
                            .defaultCount(3)
                            .maxCount(10)
                            .sortDefault(Sort.by("id").ascending())
                            .baseUrlStrategy(useRequestUrl())
                            .build())
                    .mappings(Mappings.forEntity(FugaziEntity.class).string("name").get())
                    .defaultQuery(returnNothing())
                    .build())
            .build();
    String url =
        "http://localhost/fugazi?name:contains="
            + requestName
            + "&count="
            + requestCount
            + "&page=";
    var expectedPaging =
        Paging.builder()
            .totalRecords(totalRecords)
            .totalPages(totalPages)
            .firstPage(Optional.ofNullable(firstPage))
            .previousPage(Optional.ofNullable(previousPage))
            .thisPage(Optional.ofNullable(thisPage))
            .nextPage(Optional.ofNullable(nextPage))
            .lastPage(Optional.ofNullable(lastPage))
            .firstPageUrl(Optional.ofNullable(firstPage == null ? null : url + firstPage))
            .previousPageUrl(Optional.ofNullable(previousPage == null ? null : url + previousPage))
            .thisPageUrl(Optional.ofNullable(thisPage == null ? null : url + thisPage))
            .nextPageUrl(Optional.ofNullable(nextPage == null ? null : url + nextPage))
            .lastPageUrl(Optional.ofNullable(firstPage == null ? null : url + lastPage))
            .build();
    var request = new MockHttpServletRequest();
    request.addParameter("name:contains", requestName);
    request.addParameter("page", requestPage);
    request.addParameter("count", requestCount);
    request.setRequestURI("/fugazi");
    var result = vulcan.search(request);
    assertThat(result.paging()).isEqualTo(expectedPaging);
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

  @Test
  void unknownParametersAreRemovedFromLinks() {
    var vulcan =
        Vulcan.forRepo(repo)
            .config(
                VulcanConfiguration.forEntity(FugaziEntity.class)
                    .paging(
                        PagingConfiguration.builder()
                            .pageParameter("page")
                            .countParameter("count")
                            .defaultCount(3)
                            .maxCount(10)
                            .sortDefault(Sort.by("id").ascending())
                            .baseUrlStrategy(useRequestUrl())
                            .build())
                    .mappings(Mappings.forEntity(FugaziEntity.class).string("name").get())
                    .defaultQuery(Vulcan.rejectRequest())
                    .build())
            .build();
    var request = new MockHttpServletRequest();
    request.addParameter("name:contains", "a");
    request.addParameter("pet", "dog");
    request.setRequestURI("/fugazi");
    var result = vulcan.search(request);
    assertThat(result.paging().thisPageUrl().orElse(null))
        .isEqualTo("http://localhost/fugazi?name:contains=a&count=3&page=1");
  }
}
