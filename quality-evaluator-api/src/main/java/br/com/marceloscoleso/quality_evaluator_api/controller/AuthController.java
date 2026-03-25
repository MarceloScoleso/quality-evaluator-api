package br.com.marceloscoleso.quality_evaluator_api.controller;
 
import br.com.marceloscoleso.quality_evaluator_api.dto.*;
import br.com.marceloscoleso.quality_evaluator_api.service.UserAdminService;
 
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
 
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
 
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
 
@Tag(
    name = "Authentication",
    description = "Endpoints responsáveis pelo registro e autenticação de usuários"
)
@RestController
@RequestMapping("/auth")
public class AuthController {
 
    private final UserAdminService userService;
 
   
    private final Map<String, Bucket> loginBuckets    = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
 
    public AuthController(UserAdminService userService) {
        this.userService = userService;
    }
 
    
    private Bucket getLoginBucket(String ip) {
        return loginBuckets.computeIfAbsent(ip, k ->
            Bucket.builder()
                .addLimit(Bandwidth.classic(
                    5, Refill.intervally(5, Duration.ofMinutes(1))
                ))
                .build()
        );
    }
 
    
    private Bucket getRegisterBucket(String ip) {
        return registerBuckets.computeIfAbsent(ip, k ->
            Bucket.builder()
                .addLimit(Bandwidth.classic(
                    3, Refill.intervally(3, Duration.ofHours(1))
                ))
                .build()
        );
    }
 
    
 
    @Operation(
        summary = "Registrar novo usuário",
        description = "Cria um novo usuário no sistema"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Usuário registrado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserResponseDTO.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "429", description = "Muitas requisições")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody UserRequestDTO request,
            HttpServletRequest httpRequest) {
 
        String ip = getClientIp(httpRequest);
 
        if (!getRegisterBucket(ip).tryConsume(1)) {
            return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "Muitas tentativas de cadastro. Aguarde antes de tentar novamente."));
        }
 
        return ResponseEntity.ok(userService.register(request));
    }
 
   
 
    @Operation(
        summary = "Login do usuário",
        description = "Autentica o usuário e retorna um token JWT"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Autenticação realizada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LoginResponseDTO.class)
            )
        ),
        @ApiResponse(responseCode = "401", description = "Credenciais inválidas"),
        @ApiResponse(responseCode = "429", description = "Muitas tentativas de login")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                description = "Credenciais de login",
                content = @Content(
                    schema = @Schema(implementation = LoginRequestDTO.class),
                    examples = @ExampleObject(
                        value = """
                        {
                          "email": "marcelo@email.com",
                          "password": "SenhaForte@123"
                        }
                        """
                    )
                )
            )
            @RequestBody @Valid LoginRequestDTO request,
            HttpServletRequest httpRequest) {
 
        String ip = getClientIp(httpRequest);
 
        if (!getLoginBucket(ip).tryConsume(1)) {
            return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "Muitas tentativas de login. Aguarde 1 minuto e tente novamente."));
        }
 
        String token = userService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(new LoginResponseDTO(token));
    }
 
    
 
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
           
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}