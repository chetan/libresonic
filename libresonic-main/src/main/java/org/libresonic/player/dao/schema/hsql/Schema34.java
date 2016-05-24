/*
 This file is part of Libresonic.

 Libresonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Libresonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Libresonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Libresonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.libresonic.player.dao.schema.hsql;

import org.libresonic.player.Logger;
import org.libresonic.player.dao.schema.Schema;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Used for creating and evolving the database schema.
 * This class implementes the database schema for Libresonic version 3.4.
 *
 * @author Sindre Mehus
 */
public class Schema34 extends Schema {

    private static final Logger LOG = Logger.getLogger(Schema34.class);

    @Override
    public void execute(JdbcTemplate template) {

        if (template.queryForObject("select count(*) from version where version = 10", Integer.class) == 0) {
            LOG.info("Updating database schema to version 10.");
            template.execute("insert into version values (10)");
        }

        if (!columnExists(template, "ldap_authenticated", "user")) {
            LOG.info("Database column 'user.ldap_authenticated' not found.  Creating it.");
            template.execute("alter table user add ldap_authenticated boolean default false not null");
            LOG.info("Database column 'user.ldap_authenticated' was added successfully.");
        }

        if (!columnExists(template, "party_mode_enabled", "user_settings")) {
            LOG.info("Database column 'user_settings.party_mode_enabled' not found.  Creating it.");
            template.execute("alter table user_settings add party_mode_enabled boolean default false not null");
            LOG.info("Database column 'user_settings.party_mode_enabled' was added successfully.");
        }
    }
}