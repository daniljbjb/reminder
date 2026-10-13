/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.auth_service.repository;

import dan.auth_service.model.OAuthProvider;

import dan.auth_service.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author danil
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderId(OAuthProvider provider, String providerId);

    @Query("""
            select distinct u from User u
            left join fetch u.roles
            where u.provider = :provider and u.providerId = :providerId
        """)
    Optional<User> findByProviderAndProviderIdWithRoles(
            @Param("provider") OAuthProvider provider,
            @Param("providerId") String providerId
    );
}
