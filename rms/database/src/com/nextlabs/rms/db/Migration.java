package com.nextlabs.rms.db;

import com.googlecode.flyway.core.api.migration.jdbc.JdbcMigration;
import com.nextlabs.common.util.StringUtils;

import java.lang.reflect.Method;
import java.sql.Connection;

public abstract class Migration implements JdbcMigration {

    protected String migrationClassName;

    @Override
    public void migrate(Connection connection) throws Exception {
        if (!StringUtils.hasText(migrationClassName)) {
            return;
        }

        Method method = Class.forName(migrationClassName).getMethod("migrate");
        method.invoke(Class.forName(migrationClassName).newInstance());
    }
}
