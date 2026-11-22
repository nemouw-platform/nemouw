package com.nemouw.common.exception;

import com.nemouw.common.web.TraceIdFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The handler against Spring's real exceptions, with the same snake_case JSON setting as production. */
class GlobalExceptionHandlerWebTest {

    private MockMvc mvc;

    @RestController
    @RequestMapping("/probe")
    static class ProbeController {

        record Item(@NotBlank String displayName, @Size(max = 5) String code) {
        }

        @PostMapping("/items")
        String create(@Valid @RequestBody Item item) {
            return "ok";
        }

        @GetMapping("/items/{id}")
        String find(@PathVariable UUID id) {
            return id.toString();
        }

        @GetMapping("/forbidden")
        void forbidden() {
            throw Errors.forbidden();
        }

        @GetMapping("/invalid")
        void invalid() {
            throw new ValidationException(List.of(FieldErrors.required("email")));
        }

        @GetMapping("/limited")
        void limited() {
            throw Errors.tooManyRequests(30);
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("jdbc:postgresql://db password=secret");
        }
    }

    @BeforeEach
    void setUp() {
        JsonMapper mapper = JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
        mvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new StringHttpMessageConverter(), new JacksonJsonHttpMessageConverter(mapper))
                .addFilters(new TraceIdFilter())
                .build();
    }

    private ResultActions postItem(String json) throws Exception {
        return mvc.perform(post("/probe/items").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Nested
    class HappyPaths {

        @Test
        void leavesASuccessfulRequestAlone() throws Exception {
            postItem("{\"display_name\":\"Dr\",\"code\":\"ab\"}")
                    .andExpect(status().isOk())
                    .andExpect(content().string("ok"));
        }
    }

    @Nested
    class OwnExceptions {

        @Test
        void answersAnApiExceptionWithItsStatusAndCode() throws Exception {
            mvc.perform(get("/probe/forbidden"))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        void answersAValidationExceptionWithItsFieldErrors() throws Exception {
            mvc.perform(get("/probe/invalid"))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors[0].field").value("email"))
                    .andExpect(jsonPath("$.errors[0].code").value("REQUIRED"));
        }

        @Test
        void passesTheRetryAfterHeaderThrough() throws Exception {
            mvc.perform(get("/probe/limited"))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().string("Retry-After", "30"));
        }
    }

    @Nested
    class RequestBodyErrors {

        @Test
        void saysTheBodyIsRequiredWhenItIsEmpty() throws Exception {
            mvc.perform(post("/probe/items").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
                    .andExpect(jsonPath("$.message").value("Request body is required"));
        }

        @Test
        void saysTheBodyIsNotValidJsonForBrokenJson() throws Exception {
            postItem("{\"display_name\": ")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
                    .andExpect(jsonPath("$.message").value("Request body is not valid JSON"));
        }

        @Test
        void alsoSaysNotValidJsonForValidJsonWithAWrongTypedField() throws Exception {
            postItem("{\"display_name\": {\"a\": 1}}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
                    .andExpect(jsonPath("$.message").value("Request body is not valid JSON"));
        }

        @Test
        void reportsEveryFailedConstraintWithSnakeCaseFieldNames() throws Exception {
            postItem("{\"display_name\":\"  \",\"code\":\"toolong\"}")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("display_name", "code")))
                    .andExpect(jsonPath("$.errors[?(@.field == 'display_name')].code", contains("REQUIRED")))
                    .andExpect(jsonPath("$.errors[?(@.field == 'code')].code", contains("INVALID_FORMAT")));
        }

        @Test
        void namesThePathVariableThatCouldNotBeConverted() throws Exception {
            mvc.perform(get("/probe/items/not-a-uuid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.message").value("Invalid value for 'id'"));
        }
    }

    @Nested
    class FrameworkErrors {

        @Test
        void answersAWrongMethodWith405KeepsTheAllowHeaderAndSendsJson() throws Exception {
            mvc.perform(post("/probe/items/" + UUID.randomUUID()))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(header().string("Allow", containsString("GET")))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
        }

        @Test
        void answersAnUnsupportedContentTypeWith415AndSendsJson() throws Exception {
            mvc.perform(post("/probe/items").contentType(MediaType.TEXT_PLAIN).content("x"))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
        }
    }

    @Nested
    class UnexpectedErrors {

        @Test
        void answersWith500AndNeverShowsTheInternalMessage() throws Exception {
            mvc.perform(get("/probe/boom"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.message").value("Something went wrong"))
                    .andExpect(content().string(not(containsString("secret"))));
        }
    }

    @Nested
    class TraceId {

        @Test
        void putsTheTraceIdHeaderOfTheResponseInTheBodyOfAnError() throws Exception {
            ResultActions result = mvc.perform(get("/probe/boom"));
            String header = result.andReturn().getResponse().getHeader(TraceIdFilter.HEADER);

            assertThat(header).isNotBlank();
            result.andExpect(jsonPath("$.trace_id").value(header));
        }

        @Test
        void doesTheSameForAFrameworkError() throws Exception {
            ResultActions result = mvc.perform(post("/probe/items/" + UUID.randomUUID()));
            String header = result.andReturn().getResponse().getHeader(TraceIdFilter.HEADER);

            assertThat(header).isNotBlank();
            result.andExpect(jsonPath("$.trace_id").value(header));
        }
    }
}