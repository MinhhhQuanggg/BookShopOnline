package fit.hutech.spring.services;

import fit.hutech.spring.entities.User;
import fit.hutech.spring.repositories.IUserRepository; // Kept for consistency if needed, though userService handles logic
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class OauthService extends DefaultOAuth2UserService {
    private final UserService userService;

    @Override
    // Transactional removed to avoid CGLIB final method warning
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String email = oauth2User.getAttribute("email");
        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        // Delegate transactional logic to UserService
        // Logic: Find existing, or create new. Both cases return a managed/detached
        // User entity.
        // Since Open-In-View is true, we can access lazy collections if needed, though
        // UserService should ideally return what we need.

        User user;
        var existingUser = userService.getUserWithRolesByEmail(email);
        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            String username = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
            user = userService.saveOauthUser(email, username);
            // user returned from saveOauthUser is already initialized if it's returning a
            // saved entity with roles
        }

        // Map DB roles to GrantedAuthority
        var authorities = new HashSet<GrantedAuthority>();
        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority(role.getName())));
        }

        // Fallback to "email" as the principal name for display
        return new DefaultOAuth2User(authorities, oauth2User.getAttributes(), "email");
    }
}