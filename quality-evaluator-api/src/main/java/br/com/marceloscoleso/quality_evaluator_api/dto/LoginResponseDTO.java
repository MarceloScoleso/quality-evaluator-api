package br.com.marceloscoleso.quality_evaluator_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token JWT gerado após autenticação com sucesso")
public class LoginResponseDTO {

    @Schema(description = "Token JWT para autenticação", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    public LoginResponseDTO(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
