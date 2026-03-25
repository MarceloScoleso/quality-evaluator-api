package br.com.marceloscoleso.quality_evaluator_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserRequestDTO {

    @NotBlank
    private String name;

    @Email
    @NotBlank
    private String email;

    @NotBlank
@Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
@Pattern(
    regexp = "^(?=.*[A-Z])(?=.*[0-9]).+$",
    message = "Senha deve conter ao menos uma letra maiúscula e um número"
)
private String password;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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
