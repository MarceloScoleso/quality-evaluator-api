package br.com.marceloscoleso.quality_evaluator_api.service.impl;
 
import br.com.marceloscoleso.quality_evaluator_api.dto.UserRequestDTO;
import br.com.marceloscoleso.quality_evaluator_api.dto.UserResponseDTO;
import br.com.marceloscoleso.quality_evaluator_api.exception.ResourceNotFoundException;
import br.com.marceloscoleso.quality_evaluator_api.model.User;
import br.com.marceloscoleso.quality_evaluator_api.repository.UserRepository;
import br.com.marceloscoleso.quality_evaluator_api.security.JwtService;
import br.com.marceloscoleso.quality_evaluator_api.service.UserService;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.util.List;
 
@Service
public class UserServiceImpl implements UserService {
 
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
 
   
    private static final String INVALID_CREDENTIALS_MSG = "Email ou senha incorretos";
 
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
 
    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }
 
    
 
    @Override
    @Transactional
    public UserResponseDTO register(UserRequestDTO request) {
 
        if (userRepository.existsByEmail(request.getEmail())) {
          
            throw new RuntimeException("Não foi possível completar o cadastro. Verifique os dados.");
        }
 
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
 
        User saved = userRepository.save(user);
 
        
        log.info("Novo usuário registrado. ID: {}", saved.getId());
 
        return toResponseDTO(saved);
    }
 
 
 
    @Override
    public String login(String email, String password) {
 
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(INVALID_CREDENTIALS_MSG));
 
        if (!passwordEncoder.matches(password, user.getPassword())) {
            
            throw new RuntimeException(INVALID_CREDENTIALS_MSG);
        }
 
        log.info("Login realizado. Usuário ID: {}", user.getId());
 
        return jwtService.generateToken(user.getEmail());
    }
 
   
 
    @Override
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }
 
    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuário não encontrado");
        }
        userRepository.deleteById(id);
        log.info("Usuário ID {} excluído por admin.", id);
    }
 
  
 
    @Override
    public UserResponseDTO findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        return toResponseDTO(user);
    }
 
 
    @Override
    @Transactional
    public void deleteByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
 
        userRepository.delete(user);
 
        
        log.info("Conta excluída pelo próprio titular. ID: {} (LGPD Art. 18).", user.getId());
    }
 
    
 
    private UserResponseDTO toResponseDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}