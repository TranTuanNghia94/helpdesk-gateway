package com.it.gateway.service.Security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.it.gateway.model.User.UserInfo;
import com.it.gateway.service.Redis.RedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final RedisService redisService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            log.info("Loading user by username: {}", username);

            UserInfo user = (UserInfo) redisService.get(username);

            if (user == null) {
                throw new UsernameNotFoundException("User not found");
            }
            
            return User.withUsername(user.getUsername()).build();
        } catch (Exception e) {
            log.error("Error loading user by username: {} | \nError: {}", username, e.getMessage());
            throw new UsernameNotFoundException(e.getMessage());
        }
    }
}
