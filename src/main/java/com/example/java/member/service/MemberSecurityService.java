package com.example.java.member.service;

import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.java.member.dto.LoginUserDto;
import com.example.java.member.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberSecurityService implements UserDetailsService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public UserDetails loadUserByUsername(String loginUsername) throws UsernameNotFoundException {

        String[] parts = loginUsername.split(":", 2);

        if (parts.length != 2) {
            throw new UsernameNotFoundException("로그인 유형이 없습니다.");
        }

        String loginType = parts[0];
        String username = parts[1];

        LoginUserDto user;

        if ("MEMBER".equals(loginType)) {
            user = findMember(username)
                    .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 회원 아이디입니다."));
        } else if ("SELLER".equals(loginType)) {
            user = findSeller(username)
                    .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 판매처 아이디입니다."));
        } else if ("ADMIN".equals(loginType)) {
            user = findAdmin(username)
                    .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 관리자 아이디입니다."));
        } else {
            throw new UsernameNotFoundException("잘못된 로그인 유형입니다.");
        }

        return new CustomUserDetails(user);
    }

    private Optional<LoginUserDto> findMember(String username) {

        String sql = """
                SELECT
                    seq,
                    username,
                    password,
                    role,
                    status
                FROM member
                WHERE username = :username
                """;

        return findUser(sql, username, "MEMBER");
    }

    private Optional<LoginUserDto> findSeller(String username) {

        String sql = """
                SELECT
                    seq,
                    id AS username,
                    password,
                    role,
                    status
                FROM seller
                WHERE id = :username
                """;

        return findUser(sql, username, "SELLER");
    }

    private Optional<LoginUserDto> findAdmin(String username) {

        String sql = """
                SELECT
                    seq,
                    id AS username,
                    password,
                    role,
                    adm_status AS status
                FROM admin
                WHERE id = :username
                """;

        return findUser(sql, username, "ADMIN");
    }

    private Optional<LoginUserDto> findUser(String sql, String username, String loginType) {

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("username", username);

        return jdbcTemplate.query(sql, params, rs -> {
            if (!rs.next()) {
                return Optional.empty();
            }

            return Optional.of(new LoginUserDto(
                    rs.getLong("seq"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("role"),
                    rs.getInt("status"),
                    loginType
            ));
        });
    }
}