package br.com.marceloscoleso.quality_evaluator_api.service.impl;

import br.com.marceloscoleso.quality_evaluator_api.dto.UserRequestDTO;
import br.com.marceloscoleso.quality_evaluator_api.dto.UserResponseDTO;
import br.com.marceloscoleso.quality_evaluator_api.model.User;
import br.com.marceloscoleso.quality_evaluator_api.repository.UserRepository;
import br.com.marceloscoleso.quality_evaluator_api.security.JwtService;
import br.com.marceloscoleso.quality_evaluator_api.service.UserAdminService;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserAdminServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public UserResponseDTO register(UserRequestDTO request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email ou senha incorretos");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);

        UserResponseDTO response = new UserResponseDTO();
        response.setId(savedUser.getId());
        response.setName(savedUser.getName());
        response.setEmail(savedUser.getEmail());
        response.setCreatedAt(savedUser.getCreatedAt());

        return response;
    }

    @Override
    public String login(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email ou senha incorretos"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
           throw new RuntimeException("Email ou senha incorretos");
        }

        return jwtService.generateToken(user.getEmail());
    }

    @Override
    public List<UserResponseDTO> getAllUsers() {
    return userRepository.findAll()
            .stream()
            .map(user -> {
                UserResponseDTO dto = new UserResponseDTO();
                dto.setId(user.getId());
                dto.setName(user.getName());
                dto.setEmail(user.getEmail());
                dto.setCreatedAt(user.getCreatedAt());
                return dto;
            })
            .toList();
    }   

    @Override
    public void deleteUser(Long id) {
    if (!userRepository.existsById(id)) {
        throw new RuntimeException("Email ou senha incorretos");
    }
    userRepository.deleteById(id);
    }
}
