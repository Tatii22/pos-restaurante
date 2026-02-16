package com.pos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class OpenApiConfig {

    @Bean
    public OpenAPI posOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("POS Backend API")
                        .description("API para sistema POS de restaurante")
                        .version("v1")
                        .contact(new Contact().name("POS Team"))
                        .license(new License().name("Uso interno")));
    }
}
