package com.ita.if103java.ims.dao.impl;

import com.ita.if103java.ims.dao.AccountTypeDao;
import com.ita.if103java.ims.entity.AccountType;
import com.ita.if103java.ims.exception.AccountTypeNotFoundException;
import com.ita.if103java.ims.exception.CRUDException;
import com.ita.if103java.ims.mapper.jdbc.AccountTypeRowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class AccountTypeDaoImpl implements AccountTypeDao {

    private JdbcTemplate jdbcTemplate;
    private AccountTypeRowMapper accountTypeRowMapper;

    @Autowired
    public AccountTypeDaoImpl(DataSource dataSource, AccountTypeRowMapper accountTypeRowMapper) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.accountTypeRowMapper = accountTypeRowMapper;
    }

    @Override
    public AccountType create(AccountType accountType) {
        try {
            ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneId.systemDefault());
            GeneratedKeyHolder holder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> createStatement(accountType, connection), holder);
            accountType.setId(Optional.ofNullable(holder.getKey())
                .map(Number::longValue)
                .orElseThrow(() -> new CRUDException("Error during an account type creation: " +
                    "Autogenerated key is null")));
            return accountType;
        } catch (DataAccessException e) {
            throw new CRUDException("create, id = " + accountType.getId(), e);
        }
    }

    @Override
    public AccountType findById(Long id) {
        try {
            return jdbcTemplate.queryForObject(Queries.SQL_SELECT_ACCOUNT_TYPE_BY_ID, accountTypeRowMapper, id);
        } catch (EmptyResultDataAccessException e) {
            throw new AccountTypeNotFoundException("id = " + id, e);
        } catch (DataAccessException e) {
            throw new CRUDException("get, id = " + id, e);
        }
    }

    @Override
    public AccountType findByName(String name) {
        try {
            return jdbcTemplate.queryForObject(Queries.SQL_SELECT_ACCOUNT_TYPE_BY_NAME, accountTypeRowMapper, name);
        } catch (EmptyResultDataAccessException e) {
            throw new AccountTypeNotFoundException("name = " + name, e);
        } catch (DataAccessException e) {
            throw new CRUDException("get, name = " + name, e);
        }
    }

    @Override
    public List<AccountType> selectAllActive() {
        try {
            return jdbcTemplate.query(Queries.SQL_SELECT_ALL_ACCOUNT_TYPES, accountTypeRowMapper);
        } catch (DataAccessException e) {
            throw new CRUDException("get, *", e);
        }
    }

    @Override
    public List<AccountType> selectAllPossibleToUpgrade(Long typeId) {
        try {
            return jdbcTemplate.query(Queries.SQL_FIND_ALL_POSSIBLE_TO_UPGRADE, accountTypeRowMapper, typeId);
        } catch (DataAccessException e) {
            throw new CRUDException("get, *", e);
        }
    }

    @Override
    public AccountType update(AccountType accountType) {
        int status;
        try {
            status = jdbcTemplate.update(
                Queries.SQL_UPDATE_ACCOUNT_TYPE,
                accountType.getName(),
                accountType.getPrice(),
                accountType.getLevel(),
                accountType.getMaxWarehouses(),
                accountType.getMaxWarehouseDepth(),
                accountType.getMaxUsers(),
                accountType.getMaxSuppliers(),
                accountType.getMaxClients(),
                accountType.getId());

        } catch (DataAccessException e) {
            throw new CRUDException("update, id = " + accountType.getId(), e);
        }
        if (status == 0)
            throw new AccountTypeNotFoundException("Update account type exception, id = " + accountType.getId());

        return accountType;
    }

    @Override
    public Long minLvlType() {
        try {
            return Objects.requireNonNull(jdbcTemplate.queryForObject(Queries.SQL_FIND_MIN_LVL_TYPE, accountTypeRowMapper)).getId();
        } catch (EmptyResultDataAccessException e) {
            throw new AccountTypeNotFoundException("Find min lvl type", e);
        } catch (DataAccessException e) {
            throw new CRUDException("Find min lvl type", e);
        }
    }

    @Override
    public boolean delete(Long id) {
        int status;
        try {
            status = jdbcTemplate.update(Queries.SQL_SET_ACTIVE_STATUS_ACCOUNT_TYPE, false, id);

        } catch (DataAccessException e) {
            throw new CRUDException("delete, id = " + id, e);
        }
        if (status == 0)
            throw new AccountTypeNotFoundException("Delete account type exception, id = " + id);

        return true;
    }

    private PreparedStatement createStatement(AccountType accountType, Connection connection) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(Queries.SQL_CREATE_ACCOUNT_TYPE, Statement.RETURN_GENERATED_KEYS);

        int i = 0;
        preparedStatement.setString(++i, accountType.getName());
        preparedStatement.setDouble(++i, accountType.getPrice());
        preparedStatement.setInt(++i, accountType.getMaxWarehouses());
        preparedStatement.setInt(++i, accountType.getMaxWarehouseDepth());
        preparedStatement.setInt(++i, accountType.getMaxUsers());
        preparedStatement.setInt(++i, accountType.getMaxSuppliers());
        preparedStatement.setInt(++i, accountType.getMaxClients());
        preparedStatement.setBoolean(++i, accountType.isActive());

        return preparedStatement;
    }

    class Queries {

        static final String SQL_CREATE_ACCOUNT_TYPE = """
            INSERT INTO account_types
            (name, price, level, max_warehouses, max_warehouse_depth, max_users, max_suppliers, max_clients, active)
            VALUES(?,?,?,?,?)
        """;

        static final String SQL_SELECT_ACCOUNT_TYPE_BY_ID = """
            SELECT *
            FROM account_types
            WHERE id = ?
        """;

        static final String SQL_SELECT_ACCOUNT_TYPE_BY_NAME = """
            SELECT *
            FROM account_types
            WHERE name = ?
        """;

        static final String SQL_SELECT_ALL_ACCOUNT_TYPES = """
            SELECT *
            FROM account_types
            where active = true
        """;

        static final String SQL_UPDATE_ACCOUNT_TYPE = """
            UPDATE account_types
            SET name = ?, price= ?, level= ?, max_warehouses = ?,
            max_warehouse_depth = ?, max_users = ?,
            max_suppliers = ?, max_clients = ?, active = ?
            WHERE id = ?
        """;

        static final String SQL_SET_ACTIVE_STATUS_ACCOUNT_TYPE = """
            UPDATE account_types
            SET active = ?
            WHERE id = ?
        """;

        static final String SQL_FIND_MIN_LVL_TYPE = """
            SELECT MIN(level)
            FROM account_types
        """;

        static final String SQL_FIND_ALL_POSSIBLE_TO_UPGRADE = """
            SELECT *
            FROM account_types
            WHERE level > ?
            AND active = true
        """;
    }
}