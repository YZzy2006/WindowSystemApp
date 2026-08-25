package com.window.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.window.entity.Admin;
import com.window.mapper.AdminMapper;
import com.window.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Admin login(String username, String password) {
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, username);
        Admin admin = adminMapper.selectOne(wrapper);
        if (admin == null) return null;

        if (!passwordEncoder.matches(password, admin.getPassword())) {
            return null;
        }

        if (admin.getIsFrozen() != null && admin.getIsFrozen() == 1) {
            throw new IllegalStateException("账号已被冻结，请联系管理员");
        }

        // 生成新的 tokenId，旧会话自动失效（同账号单会话保护）
        String tokenId = UUID.randomUUID().toString().replace("-", "");
        Admin up = new Admin();
        up.setId(admin.getId());
        up.setTokenId(tokenId);
        adminMapper.updateById(up);
        admin.setTokenId(tokenId);
        return admin;
    }

    public boolean validateTokenId(Integer adminId, String tokenId) {
        Admin admin = adminMapper.selectById(adminId);
        return admin != null && tokenId != null && tokenId.equals(admin.getTokenId());
    }

    @Override
    public List<Admin> listAllAdmins() {
        return adminMapper.selectList(new LambdaQueryWrapper<Admin>()
                .select(Admin::getId, Admin::getUsername, Admin::getRole, Admin::getIsFrozen)
                .orderByAsc(Admin::getId));
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("密码至少8位");
        }
        if (!password.matches(".*[a-zA-Z].*") || !password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("密码需包含字母和数字");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createAdmin(String username, String password) {
        validatePassword(password);
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, username);
        if (adminMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }
        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole("admin");
        admin.setIsFrozen(0);
        adminMapper.insert(admin);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void freezeAdmin(Integer id) {
        Admin admin = adminMapper.selectById(id);
        if (admin == null) throw new IllegalArgumentException("管理员不存在");
        if ("super_admin".equals(admin.getRole())) throw new IllegalArgumentException("不能冻结超级管理员");
        adminMapper.update(null, new LambdaUpdateWrapper<Admin>()
                .eq(Admin::getId, id)
                .set(Admin::getIsFrozen, 1)
                .set(Admin::getTokenId, null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfreezeAdmin(Integer id) {
        Admin admin = adminMapper.selectById(id);
        if (admin == null) throw new IllegalArgumentException("管理员不存在");
        Admin up = new Admin();
        up.setId(id);
        up.setIsFrozen(0);
        adminMapper.updateById(up);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Integer id, String newPassword) {
        validatePassword(newPassword);
        Admin admin = adminMapper.selectById(id);
        if (admin == null) throw new IllegalArgumentException("管理员不存在");
        adminMapper.update(null, new LambdaUpdateWrapper<Admin>()
                .eq(Admin::getId, id)
                .set(Admin::getPassword, passwordEncoder.encode(newPassword))
                .set(Admin::getTokenId, null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAdmin(Integer id, Integer currentAdminId) {
        if (id.equals(currentAdminId)) throw new IllegalArgumentException("不能删除自己");
        Admin admin = adminMapper.selectById(id);
        if (admin == null) throw new IllegalArgumentException("管理员不存在");
        if ("super_admin".equals(admin.getRole())) {
            // SQL 原子操作：只有当超级管理员数量 > 1 时才允许删除
            int deleted = adminMapper.delete(new LambdaQueryWrapper<Admin>()
                    .eq(Admin::getId, id)
                    .eq(Admin::getRole, "super_admin")
                    .apply("(SELECT COUNT(*) FROM admin WHERE role = 'super_admin') > 1"));
            if (deleted == 0) throw new IllegalArgumentException("不能删除最后一个超级管理员");
            return;
        }
        adminMapper.deleteById(id);
    }

    @Override
    public void invalidateToken(Integer adminId) {
        adminMapper.update(null, new LambdaUpdateWrapper<Admin>()
                .eq(Admin::getId, adminId)
                .set(Admin::getTokenId, null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPasswordBySuperAdmin(String targetUsername, String superAdminPassword, String newPassword) {
        validatePassword(newPassword);
        // 查找超级管理员账号
        Admin superAdmin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getRole, "super_admin").last("LIMIT 1"));
        if (superAdmin == null || !passwordEncoder.matches(superAdminPassword, superAdmin.getPassword())) {
            throw new IllegalArgumentException("超级管理员密码错误");
        }
        // 查找目标账号
        Admin target = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, targetUsername));
        if (target == null) {
            throw new IllegalArgumentException("用户名或超级管理员密码错误");
        }
        // 重置密码，清 tokenId 踢掉在线会话
        adminMapper.update(null, new LambdaUpdateWrapper<Admin>()
                .eq(Admin::getId, target.getId())
                .set(Admin::getPassword, passwordEncoder.encode(newPassword))
                .set(Admin::getTokenId, null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Integer adminId, String oldPassword, String newPassword) {
        validatePassword(newPassword);
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new IllegalArgumentException("管理员不存在");
        }
        if (!passwordEncoder.matches(oldPassword, admin.getPassword())) {
            throw new IllegalArgumentException("原密码错误");
        }
        adminMapper.update(null, new LambdaUpdateWrapper<Admin>()
                .eq(Admin::getId, adminId)
                .set(Admin::getPassword, passwordEncoder.encode(newPassword))
                .set(Admin::getTokenId, null));
    }

}
