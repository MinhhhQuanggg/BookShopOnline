package fit.hutech.spring.services;

import fit.hutech.spring.entities.Role;
import fit.hutech.spring.repositories.IRoleRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoleService {
    private final IRoleRepository roleRepository;

    /**
     * Lấy tất cả roles
     */
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    /**
     * Lấy role theo ID
     */
    public Optional<Role> getRoleById(@NotNull Long id) {
        return Optional.ofNullable(roleRepository.findRoleById(id));
    }

    /**
     * Lấy role theo tên
     */
    public Optional<Role> getRoleByName(@NotNull String name) {
        return Optional.ofNullable(roleRepository.findByName(name));
    }

    /**
     * Tạo role mới
     */
    @Transactional
    public Role createRole(@NotNull Role role) {
        if (roleRepository.findByName(role.getName()) != null) {
            throw new IllegalArgumentException("Role with name '" + role.getName() + "' already exists");
        }
        log.info("Creating new role: {}", role.getName());
        return roleRepository.save(role);
    }

    /**
     * Cập nhật role
     */
    @Transactional
    public Role updateRole(@NotNull Long id, @NotNull Role updatedRole) {
        Role existingRole = roleRepository.findRoleById(id);
        if (existingRole == null) {
            throw new IllegalArgumentException("Role not found with id: " + id);
        }

        // Check if name is being changed and if new name already exists
        if (!existingRole.getName().equals(updatedRole.getName())) {
            Role roleWithNewName = roleRepository.findByName(updatedRole.getName());
            if (roleWithNewName != null) {
                throw new IllegalArgumentException("Role with name '" + updatedRole.getName() + "' already exists");
            }
        }

        existingRole.setName(updatedRole.getName());
        existingRole.setDescription(updatedRole.getDescription());

        log.info("Updated role: {}", existingRole.getName());
        return roleRepository.save(existingRole);
    }

    /**
     * Xóa role (chỉ cho phép nếu không có user nào đang sử dụng)
     */
    @Transactional
    public void deleteRole(@NotNull Long id) {
        Role role = roleRepository.findRoleById(id);
        if (role == null) {
            throw new IllegalArgumentException("Role not found with id: " + id);
        }

        if (!role.getUsers().isEmpty()) {
            throw new IllegalStateException(
                    "Cannot delete role '" + role.getName() +
                            "' because it is assigned to " + role.getUsers().size() + " user(s)");
        }

        log.info("Deleting role: {}", role.getName());
        roleRepository.delete(role);
    }

    /**
     * Kiểm tra role có tồn tại không
     */
    public boolean existsByName(@NotNull String name) {
        return roleRepository.findByName(name) != null;
    }

    /**
     * Đếm số lượng users của một role
     */
    public int countUsersByRoleId(@NotNull Long roleId) {
        Role role = roleRepository.findRoleById(roleId);
        if (role == null) {
            return 0;
        }
        return role.getUsers().size();
    }
}
