package com.window.service;

import com.window.entity.Admin;

import java.util.List;

public interface AdminService {

    Admin login(String username, String password);

    void changePassword(Integer adminId, String oldPassword, String newPassword);

    boolean validateTokenId(Integer adminId, String tokenId);

    List<Admin> listAllAdmins();

    void createAdmin(String username, String password);

    void freezeAdmin(Integer id);

    void unfreezeAdmin(Integer id);

    void resetPassword(Integer id, String newPassword);

    void deleteAdmin(Integer id, Integer currentAdminId);

    void invalidateToken(Integer adminId);

    void resetPasswordBySuperAdmin(String targetUsername, String superAdminPassword, String newPassword);

}
