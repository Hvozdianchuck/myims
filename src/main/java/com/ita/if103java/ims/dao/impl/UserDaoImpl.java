package com.ita.if103java.ims.dao.impl;

import com.ita.if103java.ims.dao.UserDao;
import com.ita.if103java.ims.entity.User;
import com.ita.if103java.ims.exception.CRUDException;
import com.ita.if103java.ims.exception.UserNotFoundException;
import com.ita.if103java.ims.mapper.jdbc.UserRowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class UserDaoImpl implements UserDao {

    private UserRowMapper userRowMapper;
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public UserDaoImpl(JdbcTemplate jdbcTemplate, UserRowMapper userRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRowMapper = userRowMapper;
    }

    @Override
    public User create(User user) {
        try {
            GeneratedKeyHolder holder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> getPreparedStatement(user, connection), holder);
            user.setId(Optional.ofNullable(holder.getKey())
                .map(Number::longValue)
                .orElseThrow(() -> new CRUDException("Error during an user creation. Autogenerated key is null")));
            return user;
        } catch (DataAccessException e) {
            throw new CRUDException("Error during `create` user {id = " + user.getId() + "}", e);
        }
    }

    @Override
    public User findById(Long id) {
        try {
            return jdbcTemplate.queryForObject(Queries.SQL_SELECT_USER_BY_ID, userRowMapper, id);
        } catch (EmptyResultDataAccessException e) {
            throw new UserNotFoundException("Failed to obtain user during `select` {id = " + id + "}", e);
        } catch (DataAccessException e) {
            throw new CRUDException("Error during `select` user {id = " + id + "}", e);
        }

    }

    @Override
    public List<User> findUsersByAccountId(Long accountId) {
        try {
            return jdbcTemplate.query(Queries.SQL_SELECT_USERS_BY_ACCOUNT_ID, userRowMapper, accountId);
        } catch (EmptyResultDataAccessException e) {
            throw new UserNotFoundException("Failed to obtain users during `select` {accountId = " + accountId + "}", e);
        } catch (DataAccessException e) {
            throw new CRUDException("Error during `select` users {accountId = " + accountId + "}", e);
        }

    }

    @Override
    public User findAdminByAccountId(Long accountId) {
        try {
            return jdbcTemplate.queryForObject(Queries.SQL_SELECT_ADMIN_BY_ACCOUNT_ID, userRowMapper, accountId);
        } catch (EmptyResultDataAccessException e) {
            throw new UserNotFoundException("Failed to obtain user during `select` {accountId = " + accountId + "}", e);
        } catch (DataAccessException e) {
            throw new CRUDException("Error during `select` admin {accountId = " + accountId + "}", e);
        }
    }

    @Override
    public List<User> findAll() {
        try {
            return jdbcTemplate.query(Queries.SQL_SELECT_ALL_USERS, userRowMapper);
        } catch (DataAccessException e) {
            throw new CRUDException("Error during `select * ` users ", e);
        }
    }

    @Override
    public User findByEmail(String email) {
        try {
            return jdbcTemplate.queryForObject(Queries.SQL_SELECT_USER_BY_EMAIL, userRowMapper, email);
        } catch (EmptyResultDataAccessException e) {
            throw new UserNotFoundException("Failed to obtain user during `select` {email = " + email + "}", e);
        } catch (DataAccessException e) {
            throw new CRUDException("Error during `select` user {email = " + email + "}", e);
        }
    }

    @Override
    public User update(User user) {
        int status;
        try {
            ZonedDateTime updatedDateTime = ZonedDateTime.now(ZoneId.systemDefault());
            status = jdbcTemplate.update(
                Queries.SQL_UPDATE_USER,
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPassword(),
                Timestamp.from(updatedDateTime.toInstant()),
                user.isActive(),
                user.getId());

            user.setUpdatedDate(updatedDateTime);
        } catch (DataAccessException e) {
            throw new CRUDException("Error during `update` user {id = " + user.getId() + "}", e);
        }
        if (status == 0) {
            throw new UserNotFoundException("Failed to obtain user during `update` {id = " + user.getId() + "}");
        }

        return user;
    }

    @Override
    public boolean softDelete(Long id) {
        int status;
        try {
            status = jdbcTemplate.update(Queries.SQL_SET_ACTIVE_STATUS_USER, false, id);
        } catch (DataAccessException e) {
            throw new CRUDException("Error during soft `delete` user {id = " + id + "}", e);
        }
        if (status == 0) {
            throw new UserNotFoundException("Failed to obtain user during soft `delete` {id = " + id + "}");
        }

        return true;
    }

    @Override
    public boolean hardDelete(Long id) {
        int status;
        try {
            status = jdbcTemplate.update(Queries.SQL_DELETE_USER_BY_ID, id);
        } catch (DataAccessException e) {
            throw new CRUDException("Error during hard `delete` user {id = " + id + "}", e);
        }
        if (status == 0) {
            throw new UserNotFoundException("Failed to obtain user during hard `delete` {id = " + id + "}");
        }

        return true;
    }


    @Override
    public boolean updatePassword(Long id, String newPassword) {
        int status;
        try {
            ZonedDateTime updatedDateTime = ZonedDateTime.now(ZoneId.systemDefault());
            status = jdbcTemplate.update(Queries.SQL_UPDATE_PASSWORD, newPassword, updatedDateTime, id);
        } catch (DataAccessException e) {
            throw new CRUDException("Error during `update` password {id = " + id + "}", e);
        }
        if (status == 0) {
            throw new UserNotFoundException("Failed to obtain user during `update` password {id = " + id + "}");
        }


        return true;
    }

    @Override
    public User findByEmailUUID(String emailUUID) {
        try {
            return jdbcTemplate.queryForObject(Queries.SQL_SELECT_USER_BY_EMAIL_UUID, userRowMapper, emailUUID);
        } catch (EmptyResultDataAccessException e) {
            throw new UserNotFoundException("Failed to obtain user during `select` {emailUUID = " + emailUUID + "}", e);
        } catch (DataAccessException e) {
            throw new CRUDException("Error during `select` user {emailUUID = " + emailUUID + "}", e);
        }
    }

    @Override
    public Integer countOfUsers(Long accountId) {
        try {
            return jdbcTemplate.update(Queries.SQL_COUNT_OF_USERS, false, accountId);
        } catch (DataAccessException e) {
            throw new CRUDException("Error during `select count(*)` of users {accountId = " + accountId + "}", e);
        }
    }

    private PreparedStatement getPreparedStatement(User user, Connection connection) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(Queries.SQL_CREATE_USER, Statement.RETURN_GENERATED_KEYS);

        int i = 0;
        preparedStatement.setString(++i, user.getFirstName());
        preparedStatement.setString(++i, user.getLastName());
        preparedStatement.setString(++i, user.getEmail());
        preparedStatement.setString(++i, user.getPassword());
        preparedStatement.setObject(++i, user.getRole().toString());
        preparedStatement.setObject(++i, Timestamp.from(user.getCreatedDate().toInstant()));
        preparedStatement.setObject(++i, Timestamp.from(user.getUpdatedDate().toInstant()));
        preparedStatement.setBoolean(++i, false);
        preparedStatement.setString(++i, user.getEmailUUID());
        preparedStatement.setObject(++i, user.getAccountId());

        return preparedStatement;
    }

    class Queries {

        public static final String SQL_CREATE_USER = """
            INSERT INTO users
            (first_name, last_name, email,  password, role,
            created_date, updated_date, active, email_uuid, account_id)
            VALUES(?,?,?,?,?,?,?,?,?,?)
        """;

        public static final String SQL_SELECT_USER_BY_ID = """
            SELECT *
            FROM users
            WHERE id = ?
        """;

        public static final String SQL_SELECT_USER_BY_EMAIL = """
            SELECT *
            FROM users
            WHERE email = ?
        """;

        public static final String SQL_SELECT_ALL_USERS = """
            SELECT *
            FROM users
        """;

        public static final String SQL_SELECT_USERS_BY_ACCOUNT_ID = """
            SELECT *
            FROM users
            WHERE account_id = ?
        """;

        public static final String SQL_SELECT_ADMIN_BY_ACCOUNT_ID = """
            SELECT *
            FROM users
            WHERE role = 'ADMIN'
            AND account_id = ?
        """;

        public static final String SQL_UPDATE_USER = """
            UPDATE users
            SET first_name= ?, last_name = ?,
            email = ?, password = ?, updated_date = ?, active = ?
            WHERE id = ?
        """;

        public static final String SQL_SET_ACTIVE_STATUS_USER = """
            UPDATE users
            SET active = ?
            WHERE id = ?
        """;

        public static final String SQL_UPDATE_PASSWORD = """
            UPDATE users
            SET password = ?, updated_date = ?
            WHERE id = ?
        """;

        public static final String SQL_SELECT_USER_BY_EMAIL_UUID = """
            SELECT *
            FROM users
            WHERE email_uuid = ?
        """;

        public static final String SQL_DELETE_USER_BY_ID = """
            DELETE
            FROM users
            WHERE id = ?
        """;

        public static final String SQL_COUNT_OF_USERS = """
            SELECT COUNT(*)
            FROM users
            WHERE account_id = ?
        """;
    }
}
