package br.com.marceloscoleso.quality_evaluator_api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Quality Evaluator API",
                version = "v1",
                description = """
                        API responsável por avaliar a qualidade de projetos de software
                        com base em métricas técnicas, regras de negócio e critérios
                        de classificação.
                        
                        Projeto desenvolvido com foco em boas práticas de arquitetura,
                        observabilidade e design REST.
                        """,
                contact = @Contact(
                        name = "Marcelo Scoleso",
                        url = "https://github.com/marceloscoleso",
                        email = "marcelo.scolesojr@gmail.com"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        servers = {
                @Server(
                        description = "Ambiente Local",
                        url = "http://localhost:8080"
                )
        }
)
public class OpenApiConfig {
}
