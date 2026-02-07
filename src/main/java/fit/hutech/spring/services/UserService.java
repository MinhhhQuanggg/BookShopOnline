package fit.hutech.spring.services;

import fit.hutech.spring.constants.Provider;
import fit.hutech.spring.constants.Role;
import fit.hutech.spring.entities.User;
import fit.hutech.spring.repositories.IRoleRepository;
import fit.hutech.spring.repositories.IUserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class UserService implements UserDetailsService {
    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private IRoleRepository roleRepository;

    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = { Exception.class, Throwable.class })
    public void save(@NotNull User user) {
        user.setPassword(new BCryptPasswordEncoder()
                .encode(user.getPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void setDefaultRole(String username) {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }

        fit.hutech.spring.entities.Role userRole = roleRepository.findByName(Role.USER.name());
        if (userRole != null) {
            user.getRoles().add(userRole);
        } else {
            log.warn("User role not found: {}", Role.USER.name());
        }

        userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) throws UsernameNotFoundException {
        return Optional.ofNullable(userRepository.findByUsername(username));
    }

    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(userRepository.findByEmail(email));
    }

    public List<User> getAllUsers() {
        return userRepository.findAllWithRoles();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public void setSingleRole(@NotNull Long userId, @NotNull String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        fit.hutech.spring.entities.Role role = roleRepository.findByName(roleName);
        if (role == null) {
            throw new IllegalArgumentException("Role not found: " + roleName);
        }

        user.setRoles(new HashSet<>());
        user.getRoles().add(role);
        userRepository.save(user);
    }

    /**
     * Cập nhật nhiều roles cho user (thay thế toàn bộ roles hiện tại)
     */
    @Transactional
    public void updateUserRoles(@NotNull Long userId, @NotNull Set<Long> roleIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        Set<fit.hutech.spring.entities.Role> newRoles = new HashSet<>();
        for (Long roleId : roleIds) {
            fit.hutech.spring.entities.Role role = roleRepository.findRoleById(roleId);
            if (role == null) {
                throw new IllegalArgumentException("Role not found with id: " + roleId);
            }
            newRoles.add(role);
        }

        user.setRoles(newRoles);
        userRepository.save(user);
        log.info("Updated roles for user {}: {} roles", user.getUsername(), newRoles.size());
    }

    /**
     * Thêm một role cho user (giữ nguyên các roles hiện tại)
     */
    @Transactional
    public void addRoleToUser(@NotNull Long userId, @NotNull String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        fit.hutech.spring.entities.Role role = roleRepository.findByName(roleName);
        if (role == null) {
            throw new IllegalArgumentException("Role not found: " + roleName);
        }

        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }

        user.getRoles().add(role);
        userRepository.save(user);
        log.info("Added role {} to user {}", roleName, user.getUsername());
    }

    /**
     * Xóa một role khỏi user
     */
    @Transactional
    public void removeRoleFromUser(@NotNull Long userId, @NotNull String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        fit.hutech.spring.entities.Role role = roleRepository.findByName(roleName);
        if (role == null) {
            throw new IllegalArgumentException("Role not found: " + roleName);
        }

        user.getRoles().remove(role);
        userRepository.save(user);
        log.info("Removed role {} from user {}", roleName, user.getUsername());
    }

    /**
     * Kiểm tra user có role cụ thể không
     */
    public boolean hasRole(@NotNull Long userId, @NotNull String roleName) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }

    /**
     * Lấy danh sách users theo role
     */
    public List<User> getUsersByRole(@NotNull String roleName) {
        fit.hutech.spring.entities.Role role = roleRepository.findByName(roleName);
        if (role == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(role.getUsers());
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        // Eagerly fetch roles inside transaction to avoid LazyInitializationException
        Hibernate.initialize(user.getRoles());

        return user; //
    }

    @Transactional
    public User saveOauthUser(String email, @NotNull String username) {
        if (userRepository.existsByEmail(email)) {
            return userRepository.findByEmail(email);
        }

        String uniqueUsername = username;
        int suffix = 1;
        while (userRepository.existsByUsername(uniqueUsername)) {
            uniqueUsername = username + "_" + suffix;
            suffix++;
        }

        User user = new User();
        user.setUsername(uniqueUsername);
        user.setEmail(email);
        user.setPassword(new BCryptPasswordEncoder().encode(UUID.randomUUID().toString()));
        user.setProvider(Provider.GOOGLE.value);

        user.setRoles(new HashSet<>());
        user.getRoles().add(roleRepository.findByName(Role.USER.name()));

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUserById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UsernameNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public Optional<User> getUserWithRolesByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            Hibernate.initialize(user.getRoles());
            return Optional.of(user);
        }
        return Optional.empty();
    }

    @Transactional
    public Optional<User> getUserWithRolesByUsername(String username) {
        User user = userRepository.findByUsername(username);
        if (user != null) {
            Hibernate.initialize(user.getRoles());
            return Optional.of(user);
        }
        return Optional.empty();
    }
}
