package fit.hutech.spring.config;

import fit.hutech.spring.entities.Role;
import fit.hutech.spring.entities.User;
import fit.hutech.spring.repositories.IRoleRepository;
import fit.hutech.spring.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * DataLoader để khởi tạo dữ liệu roles mặc định khi ứng dụng khởi động
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RoleDataLoader implements CommandLineRunner {
    private final IRoleRepository roleRepository;
    private final IUserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        initializeRoles();
        removeGuestRole();
    }

    private void initializeRoles() {
        log.info("Initializing default roles...");

        // ADMIN role
        if (roleRepository.findByName("ADMIN") == null) {
            Role admin = Role.builder()
                    .name("ADMIN")
                    .description("Quản trị viên - Toàn quyền quản lý hệ thống")
                    .build();
            roleRepository.save(admin);
            log.info("Created ADMIN role");
        }

        // USER role
        if (roleRepository.findByName("USER") == null) {
            Role user = Role.builder()
                    .name("USER")
                    .description("Người dùng - Có thể mua sách và quản lý tài khoản")
                    .build();
            roleRepository.save(user);
            log.info("Created USER role");
        }

        log.info("Role initialization completed. Total roles: {}", roleRepository.count());
    }

    private void removeGuestRole() {
        Role guestRole = roleRepository.findByName("GUEST");
        if (guestRole != null) {
            log.warn("Found GUEST role in database. Proceeding to delete...");

            // Break relationships first because Role has CascadeType.ALL (dangerous!)
            // We must traverse users and remove the role from them.
            if (guestRole.getUsers() != null) {
                List<User> usersWithGuestRole = new ArrayList<>(guestRole.getUsers());
                for (User user : usersWithGuestRole) {
                    if (user.getRoles().contains(guestRole)) {
                        user.getRoles().remove(guestRole);
                        userRepository.save(user); // Update join table
                    }
                }
                // Clear collection on the Role side to prevent CascadeType.ALL from deleting
                // Users
                guestRole.getUsers().clear();
            }

            roleRepository.delete(guestRole);
            log.info("Successfully deleted GUEST role from database.");
        }
    }
}


