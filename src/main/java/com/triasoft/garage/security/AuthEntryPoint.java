package com.triasoft.garage.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.constants.Errors;
import com.triasoft.garage.model.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;

@Component
public class AuthEntryPoint implements AuthenticationEntryPoint, Serializable {

	@Serial
	private static final long serialVersionUID = -7858869558953243875L;

	public static final String AUTH_ERROR_ATTR = "com.triasoft.garage.AUTH_ERROR";

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
						 AuthenticationException authException) throws IOException {

		Errors error = request.getAttribute(AUTH_ERROR_ATTR) instanceof Errors classified
				? classified
				: ErrorCode.Security.INVALID_TOKEN;

		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

		ApiResponse<?> body = ApiResponse.error(error.getCode(), error.getMessage(), request.getServletPath());
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
