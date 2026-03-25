package br.com.marceloscoleso.quality_evaluator_api.controller;

import br.com.marceloscoleso.quality_evaluator_api.dto.UserResponseDTO;
import br.com.marceloscoleso.quality_evaluator_api.service.UserAdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin Users", description = "Endpoints administrativos de usuários")
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserAdminService userService;

    public AdminUserController(UserAdminService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Listar todos os usuários")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')") 
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(summary = "Deletar usuário pelo ID")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}