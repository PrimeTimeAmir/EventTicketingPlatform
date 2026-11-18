package kz.kimep.ticketing.identity.service;

import kz.kimep.ticketing.identity.api.dto.AuthResponse;
import kz.kimep.ticketing.identity.api.dto.LoginRequest;
import kz.kimep.ticketing.identity.api.dto.RegisterRequest;
import kz.kimep.ticketing.identity.domain.Role;
import kz.kimep.ticketing.identity.domain.User;
import kz.kimep.ticketing.identity.repo.UserRepository;
import kz.kimep.ticketing.identity.security.JwtService;
import kz.kimep.ticketing.support.audit.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered"
            );
        }

        User user = new User(
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                Role.CUSTOMER,
                true
        );

        User savedUser = userRepository.save(user);

        auditService.record(
                savedUser.getId(),
                "USER_REGISTERED",
                "USER",
                savedUser.getId().toString()
        );

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(email);

        String accessToken =
                jwtService.generateAccessToken(userDetails);

        String refreshToken =
                refreshTokenService.createRefreshToken(savedUser);

        return new AuthResponse(
                accessToken,
                "Bearer",
                refreshToken
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            request.password()
                    )
            );

        } catch (AuthenticationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"
                ));

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(email);

        String accessToken =
                jwtService.generateAccessToken(userDetails);

        String refreshToken =
                refreshTokenService.createRefreshToken(user);

        // Record successful login in audit history
        auditService.record(
                user.getId(),
                "USER_LOGGED_IN",
                "USER",
                user.getId().toString()
        );

        return new AuthResponse(
                accessToken,
                "Bearer",
                refreshToken
        );
    }

    public AuthResponse refresh(String rawRefreshToken) {

        RefreshTokenService.RotationResult result =
                refreshTokenService.rotateRefreshToken(rawRefreshToken);

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(result.email());

        String accessToken =
                jwtService.generateAccessToken(userDetails);

        return new AuthResponse(
                accessToken,
                "Bearer",
                result.rawToken()
        );
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeRefreshToken(rawRefreshToken);
    }
}
