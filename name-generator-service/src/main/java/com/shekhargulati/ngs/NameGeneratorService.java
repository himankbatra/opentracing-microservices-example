package com.shekhargulati.ngs;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.internal.samplers.ProbabilisticSampler;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

import static strman.Strman.toKebabCase;

@SpringBootApplication
public class NameGeneratorService {

    public static void main(String[] args) {
        SpringApplication.run(NameGeneratorService.class, args);
    }

    @Bean
    public Tracer tracer() {
        SamplerConfiguration samplerConfig = SamplerConfiguration.fromEnv()
                .withType(ConstSampler.TYPE)
                .withParam(1);

        ReporterConfiguration reporterConfig = ReporterConfiguration.fromEnv()
                .withLogSpans(true);

        Configuration config = new Configuration("name-svc")
                .withSampler(samplerConfig)
                .withReporter(reporterConfig);

        return config.getTracer();
    }

}

@RestController
@RequestMapping("/api/v1/names")
class NameResource {

    OkHttpClient client = new OkHttpClient();

    @Autowired
    private Tracer tracer;

    @GetMapping(path = "/random")
    public String name() throws Exception {

        Scope span = tracer.buildSpan("generate-name").startActive(true);

        Span scientistSpan = tracer.buildSpan("scientist-name-service").asChildOf(span.span()).start();
        String scientist = makeRequest("http://localhost:8090/api/v1/scientists/random");
        scientistSpan.finish();

        Span animalSpan = tracer.buildSpan("animal-name-service").asChildOf(span.span()).start();
        String animal = makeRequest("http://localhost:9000/api/v1/animals/random");
        animalSpan.finish();

        String name = toKebabCase(scientist) + "-" + toKebabCase(animal);
        span.close();
        return name;
    }

    private String makeRequest(String url) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url);

        tracer.inject(
                tracer.activeSpan().context(),
                Format.Builtin.HTTP_HEADERS,
                new RequestBuilderCarrier(requestBuilder)

        );

        Request request = requestBuilder
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }


}