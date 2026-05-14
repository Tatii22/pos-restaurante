package com.pos.config;


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

