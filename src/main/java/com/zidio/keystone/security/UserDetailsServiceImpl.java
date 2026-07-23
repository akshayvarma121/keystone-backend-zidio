package com.zidio.keystone.security;

import com.zidio.keystone.domain.User;
import com.zidio.keystone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        UUID customerId = user.getCustomer() != null ? user.getCustomer().getId() : null;

        return new KeystoneUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                customerId,
                user.getActive()
        );
    }
}
