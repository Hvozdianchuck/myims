package com.ita.if103java.ims.dao;

import com.ita.if103java.ims.entity.User;

import java.util.List;

public interface UserDao {

    User create(User user);

    User findById(Long id);

    List<User> findUsersByAccountId(Long accountId);

    User findAdminByAccountId(Long accountId);

    User update(User user);

    boolean softDelete(Long id);

    boolean hardDelete(Long id);

    List<User> findAll();

    User findByEmail(String email);

    boolean updatePassword(Long id, String newPassword);

    User findByEmailUUID(String emailUUID);

    Integer countOfUsers(Long accountId);
}