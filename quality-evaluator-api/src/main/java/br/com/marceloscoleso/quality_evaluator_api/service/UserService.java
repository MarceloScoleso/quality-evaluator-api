package br.com.marceloscoleso.quality_evaluator_api.service;

import br.com.marceloscoleso.quality_evaluator_api.dto.UserRequestDTO;
import br.com.marceloscoleso.quality_evaluator_api.dto.UserResponseDTO;

public interface UserService {

    UserResponseDTO register(UserRequestDTO request);

    String login(String email, String password);
}
