package br.com.marceloscoleso.quality_evaluator_api.controller;
 
import br.com.marceloscoleso.quality_evaluator_api.dto.UserResponseDTO;
import br.com.marceloscoleso.quality_evaluator_api.service.UserService;
 
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
 
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
 
@Tag(name = "User", description = "Endpoints do usuário autenticado")
@RestController
@RequestMapping("/api/user")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
 
    private final UserService userService;
 
    public UserController(UserService userService) {
        this.userService = userService;
    }
 
    @Operation(
        summary = "Ver meus dados",
        description = "Retorna os dados pessoais do usuário autenticado"
    )
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMyData() {
        String email = getAuthenticatedEmail();
        return ResponseEntity.ok(userService.findByEmail(email));
    }
 
    @Operation(
        summary = "Excluir minha conta",
        description = "Exclui permanentemente a conta e todos os dados do usuário autenticado"
    )
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount() {
        String email = getAuthenticatedEmail();
        userService.deleteByEmail(email);
        return ResponseEntity.noContent().build();
    }
 
    private String getAuthenticatedEmail() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
    }
}