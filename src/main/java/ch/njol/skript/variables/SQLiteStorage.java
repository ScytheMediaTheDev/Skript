/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright Peter Güttinger, SkriptLang team and contributors
 */
package ch.njol.skript.variables;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.BiFunction;

import org.eclipse.jdt.annotation.Nullable;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;

import com.zaxxer.hikari.HikariConfig;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.util.NonNullPair;

@Deprecated
@ScheduledForRemoval
public class SQLiteStorage extends JdbcStorage {

	SQLiteStorage(String name) {
		super(name, "CREATE TABLE IF NOT EXISTS %s (" +
				"name         VARCHAR(" + MAX_VARIABLE_NAME_LENGTH + ")  NOT NULL  PRIMARY KEY," +
				"type         VARCHAR(" + MAX_CLASS_CODENAME_LENGTH + ")," +
				"value        BLOB(" + MAX_VALUE_SIZE + ")" +
				");");
	}

	@Override
	@Nullable
	public HikariConfig configuration(SectionNode config) {
		File file = this.file;
		if (file == null)
			return null;
		setTableName(config.get("table", "variables21"));
		String name = file.getName();
		assert name.endsWith(".db");

		HikariConfig configuration = new HikariConfig();
		configuration.setJdbcUrl("jdbc:sqlite:" + (file == null ? ":memory:" : file.getAbsolutePath()));
		return configuration;
	}

	@Override
	protected boolean requiresFile() {
		return true;
	}

	@Override
	protected File getFile(String file) {
		if (!file.endsWith(".db"))
			file = file + ".db"; // required by SQLibrary
		return new File(file);
	}

	@Override
	protected String getReplaceQuery() {
		return "REPLACE INTO " + getTableName() + " (name, type, value, update_guid) VALUES (?, ?, ?, ?)";
	}

	@Override
	@Nullable
	protected NonNullPair<String, String> getMonitorQueries() {
		return null;
	}

	@Override
	protected String getSelectQuery() {
		return "SELECT name, type, value from " + getTableName();
	}

	@Override
	protected BiFunction<Integer, ResultSet, SerializedVariable> get() {
		return (index, result) -> {
			int i = 1;
			try {
				String name = result.getString(i++);
				if (name == null) {
					Skript.error("Variable with NULL name found in the database '" + databaseName + "', ignoring it");
					return null;
				}
				String type = result.getString(i++);
				byte[] value = result.getBytes(i++);
				return new SerializedVariable(name, type, value);
			} catch (SQLException e) {
				Skript.exception(e, "Failed to collect variable from database.");
				return null;
			}
		};
	}

}