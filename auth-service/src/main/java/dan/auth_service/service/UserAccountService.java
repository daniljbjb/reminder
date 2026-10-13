/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.auth_service.service;


import dan.auth_service.model.OAuthProvider;
import dan.auth_service.model.Role;
import dan.auth_service.model.User;
import dan.auth_service.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 *
 * @author danil
 */
@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserRepository userRepository;

    @Transactional
    public User getOrCreateByGoogle(String sub, String email) {
        return userRepository
                .findByProviderAndProviderIdWithRoles(OAuthProvider.GOOGLE, sub)
                .orElseGet(() -> createGoogleUserOrLoadIfRace(sub, email));
    }

    private User createGoogleUserOrLoadIfRace(String sub, String email) {
        try {
            User user = User.builder()
                    .provider(OAuthProvider.GOOGLE)
                    .providerId(sub)
                    .email(email)
                    .roles(Set.of(Role.USER))
                    .build();

            return userRepository.save(user);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // кто-то уже успел создать пользователя параллельно
            return userRepository.findByProviderAndProviderIdWithRoles(OAuthProvider.GOOGLE, sub)
                    .orElseThrow(() -> e);
        }
    }
}
