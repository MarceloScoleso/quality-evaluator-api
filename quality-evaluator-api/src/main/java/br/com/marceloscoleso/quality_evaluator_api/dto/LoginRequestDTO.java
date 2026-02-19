package br.com.marceloscoleso.quality_evaluator_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados necessários para autenticação do usuário")
public class LoginRequestDTO {

    @Schema(description = "Email do usuário", example = "marcelo@email.com")
    @NotBlank
    private String email;

    @Schema(description = "Senha do usuário", example = "123456")
    @NotBlank
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
