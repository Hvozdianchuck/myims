package com.ita.if103java.ims.dao.impl;

import com.ita.if103java.ims.dao.UserDao;
import com.ita.if103java.ims.entity.User;
import com.ita.if103java.ims.exception.CRUDException;
import com.ita.if103java.ims.exception.EntityNotFoundException;
import com.ita.if103java.ims.mapper.jdbc.UserRowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserDaoImpl implements UserDao {

    private static Logger logger = LoggerFactory.getLogger(UserDaoImpl.class);
    private UserRowMapper userRowMapper;
    private JdbcTemplate jdbcTemplate;
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    public UserDaoImpl(DataSource dataSource, UserRowMapper userRowMapper) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.bCryptPasswordEncoder = new BCryptPasswordEncoder();
        this.userRowMapper = userRowMapper;
    }


    @Override
    public User create(User user) {
        try {
            ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneId.systemDefault());
            String emailUUID = UUID.randomUUID().toString();
            String encryptedPassword = bCryptPasswordEncoder.encode(user.getPassword());
            GeneratedKeyHolder holder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> createStatement(user, encryptedPassword, currentDateTime, emailUUID, connection), holder);
            user.setCreatedDate(currentDateTime);
            user.setUpdatedDate(currentDateTime);
            user.setEmailUUID(emailUUID);
            user.setId(Optional.ofNullable(holder.getKey())
                .map(Number::longValue)
                .orElseThrow(() -> new CRUDException("Error during an user creation: " +
                    "Autogenerated key is null")));
            return user;
        } catch (DataAccessException e) {
            throw crudException(e.getMessage(), "create", "id = " + user.getId());
        }
    }

    @Override
    public User findById(Long id) {
        try {
            return jdbcTemplate.queryForObject(Queries.SQL_SELECT_USER_BY_ID, userRowMapper, id);
        } catch (EmptyResultDataAccessException e) {
            throw userEntityNotFoundException(e.getMessage(), "id = " + id);
        } catch (DataAccessException e) {
            throw crudException(e.getMessage(), "get", "id = " + id);
        }

    }

    @Override
    public User findByAccountId(Long accountId) {
        try {
            return jdbcTemplate.queryForObject(Queries.SQL_SELECT_USER_BY_ACCOUNT_ID, userRowMapper, accountId);
        } catch (EmptyResultDataAccessException e) {
            throw userEntityNotFoundException(e.getMessage(), "id = " + accountId);
        } catch (DataAccessException e) {
            throw crudException(e.getMessage(), "get", "id = " + accountId);
        }

    }

    @Override
    public List<User> findAll() {
        try {
            return jdbcTemplate.query(Queries.SQL_SELECT_ALL_USERS, userRowMapper);
        } catch (DataAccessException e) {
            throw crudException(e.getMessage(), "get", "*");
        }
    }

    @Override
    public User findByEmail(String email) {
        try {
            return jdbcTemplate.queryForObject(Queries.SQL_SELECT_USER_BY_EMAIL, userRowMapper, email);
        } catch (EmptyResultDataAccessException e) {
            throw userEntityNotFoundException(e.getMessage(), "email = " + email);
        } catch (DataAccessException e) {
            throw crudException(e.getMessage(), "get", "email = " + email);
        }
    }

    @Override
    public User update(User user) {
        int status;
        try {
            ZonedDateTime updatedDateTime = ZonedDateTime.now(ZoneId.systemDefault());;
            status = jdbcTemplate.update(
                Queries.SQL_UPDATE_USER,
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPassword(),
                updatedDateTime,
                user.getId());

            user.setUpdatedDate(updatedDateTime);
        } catch (DataAccessException e) {
            throw crudException(e.getMessage(), "update", "id = " + user.getId());
        }
        if (status == 0)
            throw userEntityNotFoundException("Update user exception", "id = " + user.getId());

        return user;
    }

    @Override
    public boolean delete(Long id) {
        int status;
        try {
            status = jdbcTemplate.update(Queries.SQL_SET_ACTIVE_STATUS_USER, false, id);

        } catch (DataAccessException e) {
            throw crudException(e.getMessage(), "delete", "id = " + id);
        }
        if (status == 0)
            throw userEntityNotFoundException("Delete user exception", "id = " + id);

        return true;
    }

    @Override
    public boolean updatePassword(Long id, String newPassword) {
        int status;
        try {
            String encryptedPassword = bCryptPasswordEncoder.encode(newPassword);
            status = jdbcTemplate.update(Queries.SQL_UPDATE_PASSWORD, encryptedPassword, id);

        } catch (DataAccessException e) {
            throw crudException(e.getMessage(), "update", "id = " + id);
        }
        if (status == 0)
            throw userEntityNotFoundException("Update user password exception", "id = " + id);

        return true;
    }

    private PreparedStatement createStatement(User user, String encryptedPassword, ZonedDateTime currentDateTime,
                                              String emailUUID, Connection connection) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(Queries.SQL_CREATE_USER, Statement.RETURN_GENERATED_KEYS);

        int i = 0;
        preparedStatement.setString(++i, user.getFirstName());
        preparedStatement.setString(++i, user.getLastName());
        preparedStatement.setString(++i, user.getEmail());
        preparedStatement.setString(++i, encryptedPassword);
        preparedStatement.setObject(++i, user.getRole());
        preparedStatement.setObject(++i, currentDateTime);
        preparedStatement.setObject(++i, currentDateTime);
        preparedStatement.setBoolean(++i, user.isActive());
        preparedStatement.setString(++i, emailUUID);

        return preparedStatement;
    }

    private EntityNotFoundException userEntityNotFoundException(String message, String attribute) {
        EntityNotFoundException exception = new EntityNotFoundException(message);
        logger.error("EntityNotFoundException exception. User is not found ({}). Message: {}", attribute, message);
        return exception;
    }

    private CRUDException crudException(String message, String operation, String attribute) {
        CRUDException exception = new CRUDException(message);
        logger.error("CRUDException exception. Operation:({}) user ({}) exception. Message: {}", operation, attribute, message);
        return exception;
    }

    class Queries {

        static final String SQL_CREATE_USER = "" +
            "INSERT INTO users(first _name, last_name, email, password, role, created_date, updated_date, active, email_uuid)" +
            "VALUES(?,?,?,?,?,?,?,?,?)";

        static final String SQL_SELECT_USER_BY_ID = "SELECT * FROM users WHERE id = ?";

        static final String SQL_SELECT_USER_BY_EMAIL = "SELECT * FROM users WHERE email = ?";

        static final String SQL_SELECT_ALL_USERS = "SELECT * FROM users";

        static final String SQL_SELECT_USER_BY_ACCOUNT_ID = "SELECT * FROM users WHERE account_id = ?";

        static final String SQL_UPDATE_USER = "UPDATE users SET " +
            "first_name= ?, last_name = ?," +
            "email = ?, password = ?," +
            "updated_date = ? WHERE id = ?";

        static final String SQL_SET_ACTIVE_STATUS_USER = "UPDATE users SET active = ? WHERE id = ?";

        static final String SQL_UPDATE_PASSWORD = "UPDATE users SET password = ? WHERE id = ?";
    }
}
