package com.SecBank.pro.Security;

import com.SecBank.pro.Exceptions.ResourceNotFoundException;
import com.SecBank.pro.Models.Utilisateur;
import com.SecBank.pro.Repositories.UserRepo;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomUserDetailsServices implements UserDetailsService {
    private final UserRepo userRepo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilisateur utilisateur=userRepo.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("Utilisateur introuvable :"+email));
        return User.builder()
                .username(utilisateur.getEmail())
                .password(utilisateur.getPassword())
                .roles(utilisateur.getRole().toString())
                .disabled(!utilisateur.isActif())
                .build();

    }
}
