package com.akash.credential_verification.Configuration;

import com.akash.credential_verification.Model.StudentAuth;
import com.akash.credential_verification.Model.StudentPrincipal;
import com.akash.credential_verification.Model.University;
import com.akash.credential_verification.Model.UniversityPrincipal;
import com.akash.credential_verification.Model.User;
import com.akash.credential_verification.Model.UserPrincipal;
import com.akash.credential_verification.Repository.StudentAuthRepository;
import com.akash.credential_verification.Repository.UniversityRepository;
import com.akash.credential_verification.Repository.UserRepository;
import com.akash.credential_verification.Util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UniversityRepository universityRepo;
    private final UserRepository userRepo;
    private final StudentAuthRepository studentAuthRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7);
                Claims claims = jwtUtil.extractClaims(token);
                String subject = claims.getSubject();
                String role = (String) claims.get("role");

                if ("UNIVERSITY".equals(role)) {
                    University uni = universityRepo.findById(subject).orElseThrow();
                    var principal = new UniversityPrincipal(
                            subject,
                            uni.getMspId(),
                            uni.getName()
                    );
                    var auth = new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(new SimpleGrantedAuthority("ROLE_UNIVERSITY"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else if ("SUPER_ADMIN".equals(role)) {
                    User user = userRepo.findById(subject).orElseThrow();
                    var principal = new UserPrincipal(
                            subject,
                            user.getRole().name()
                    );
                    var auth = new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else if ("STUDENT".equals(role)) {
                    StudentAuth studentAuth = studentAuthRepo.findById(subject).orElseThrow();
                    var principal = new StudentPrincipal(
                            subject,
                            studentAuth.getEmail(),
                            studentAuth.getFirstName() + " " + studentAuth.getLastName()
                    );
                    var auth = new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignored) {}
        }
        chain.doFilter(req, res);
    }
}
