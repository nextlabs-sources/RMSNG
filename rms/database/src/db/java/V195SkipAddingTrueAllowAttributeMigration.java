package db.java;

import com.googlecode.flyway.core.api.MigrationVersion;
import com.googlecode.flyway.core.api.migration.MigrationInfoProvider;
import com.nextlabs.rms.db.Migration;

import java.sql.Connection;

public class V195SkipAddingTrueAllowAttributeMigration extends Migration implements MigrationInfoProvider {

    @Override
    public void migrate(Connection connection) throws Exception {
        migrationClassName = "com.nextlabs.rms.migration.V195SkipAddingTrueAllowAttributeMigration";
        super.migrate(connection);
    }

    @Override
    public MigrationVersion getVersion() {
        return new MigrationVersion("1.95");
    }

    @Override
    public String getDescription() {
        return "Add SkipAddingTrueAllowAttribute";
    }
}
