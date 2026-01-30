package com.triasoft.garage.service;

import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.model.login.LoginRq;
import com.triasoft.garage.model.login.LoginRs;
import com.triasoft.garage.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserService userService;
    private final EncryptionUtil encryptionUtil;

    public LoginRs login(LoginRq request) {
        LoginRs rs = new LoginRs();
        String password = encryptionUtil.decrypt(request.getPassword());
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), password));
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        UserDTO user = userService.loadUser(userPrincipal.getUsername());
        rs.setToken(tokenService.generateToken(userPrincipal,user));
        rs.setRefreshToken(tokenService.generateRefreshToken());
        rs.setFullName(userPrincipal.getUsername());
        return rs;
    }
}
