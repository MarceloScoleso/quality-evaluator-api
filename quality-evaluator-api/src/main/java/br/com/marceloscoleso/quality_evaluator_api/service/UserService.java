package br.com.marceloscoleso.quality_evaluator_api.service;

import br.com.marceloscoleso.quality_evaluator_api.dto.UserRequestDTO;
import br.com.marceloscoleso.quality_evaluator_api.dto.UserResponseDTO;

import java.util.List;

public interface UserService {

    UserResponseDTO register(UserRequestDTO request);

    String login(String email, String password);

    List<UserResponseDTO> getAllUsers();

    void deleteUser(Long id);
}