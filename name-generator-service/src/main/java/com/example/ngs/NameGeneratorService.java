package com.example.ngs;

import feign.Client;
import feign.Feign;
import feign.RequestLine;
import feign.opentracing.TracingClient;
import io.opentracing.Tracer;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static strman.Strman.toKebabCase;

@SpringBootApplication
public class NameGeneratorService {

    public static void main(String[] args) {
        SpringApplication.run(NameGeneratorService.class, args);
    }

}

@Configuration
@Import(FeignClientsConfiguration.class)
class FeignConfiguration {

    @Bean
    public OkHttpClient client() {
        return new OkHttpClient();
    }


}


interface ScientistServiceClient {

    @RequestLine("GET /api/v1/scientists/random")
    String randomScientistName();

}

interface AnimalServiceClient {

    @RequestLine("GET /api/v1/animals/random")
    String randomAnimalName();

}


@RestController
@RequestMapping("/api/v1/names")
class NameResource {


    private AnimalServiceClient animalServiceClient;

    private ScientistServiceClient scientistServiceClient;


    public NameResource(Client client, Tracer tracer, @Value("${animal.service.prefix.url}") String animalServiceUrl
            , @Value("${scientist.service.prefix.url}") String scientistServiceUrl) {
        this.animalServiceClient = Feign.builder().
                client(new TracingClient(client, tracer))
                .target(AnimalServiceClient.class, animalServiceUrl);
        this.scientistServiceClient = Feign.builder().
                client(new TracingClient(client, tracer))
                .target(ScientistServiceClient.class, scientistServiceUrl);
    }

    @GetMapping(path = "/random")
    public String name() throws Exception {
        String animal = animalServiceClient.randomAnimalName();
        String scientist = scientistServiceClient.randomScientistName();
        String name = toKebabCase(scientist) + "-" + toKebabCase(animal);
        return name;
    }


}