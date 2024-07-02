package com.alkl1m.auditlogspringbootautoconfigure.advice;

import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@ExtendWith(SpringExtension.class)
@WebMvcTest(HttpResponseLoggingAdviceTest.TestController.class)
@ContextConfiguration(classes = {HttpRequestLoggingAdviceTest.TestController.class, HttpRequestLoggingAdvice.class})
@AutoConfigureMockMvc
@AutoConfigureWebClient
class HttpRequestLoggingAdviceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void textHttpRequestLoggingAdvice_withRequestBody_LogsAreValid() throws Exception {
        var loggerContext = LoggerContext.getContext(false);
        var logger = (Logger) loggerContext.getLogger(HttpRequestLoggingAdvice.class);
        var appender = new ListAppender("List");
        appender.start();
        loggerContext.getConfiguration().addLoggerAppender(logger, appender);

        ResultActions resultActions = this.mockMvc.perform(post("/test2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\": \"value\"}"));
        resultActions.andDo(print());

        List<String> loggedStrings =
                appender.getEvents().stream().map(event -> event.getMessage().toString()).collect(Collectors.toList());
        assertTrue(loggedStrings.contains("Request body: \"{\\\"key\\\": \\\"value\\\"}\""));
    }

    @RestController
    public static class TestController {

        @PostMapping("/test2")
        public String test(@RequestBody String key) {
            return "hello";
        }

    }

}
