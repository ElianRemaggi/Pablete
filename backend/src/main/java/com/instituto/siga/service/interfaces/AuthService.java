package com.instituto.siga.service.interfaces;

import com.instituto.siga.dto.request.LoginRequest;
import com.instituto.siga.dto.request.RefreshTokenRequest;
import com.instituto.siga.dto.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);
}
