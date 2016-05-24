/*
 * This file is part of Libresonic.
 *
 *  Libresonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Libresonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Libresonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2014 (C) Sindre Mehus
 */
package org.libresonic.player.dao.schema.hsql;

import org.libresonic.player.Logger;
import org.libresonic.player.dao.schema.Schema;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Used for creating and evolving the database schema.
 * This class implements the database schema for Libresonic version 5.1.
 *
 * @author Sindre Mehus
 */
public class Schema51 extends Schema {

    private static final Logger LOG = Logger.getLogger(Schema51.class);

    @Override
    public void execute(JdbcTemplate template) {

        if (template.queryForObject("select count(*) from version where version = 23", Integer.class) == 0) {
            LOG.info("Updating database schema to version 23.");
            template.execute("insert into version values (23)");
        }

        if (!columnExists(template, "show_artist_info", "user_settings")) {
            LOG.info("Database column 'user_settings.show_artist_info' not found.  Creating it.");
            template.execute("alter table user_settings add show_artist_info boolean default true not null");
            LOG.info("Database column 'user_settings.show_artist_info' was added successfully.");
        }

        if (!columnExists(template, "auto_hide_play_queue", "user_settings")) {
            LOG.info("Database column 'user_settings.auto_hide_play_queue' not found.  Creating it.");
            template.execute("alter table user_settings add auto_hide_play_queue boolean default true not null");
            LOG.info("Database column 'user_settings.auto_hide_play_queue' was added successfully.");
        }

        if (!columnExists(template, "view_as_list", "user_settings")) {
            LOG.info("Database column 'user_settings.view_as_list' not found.  Creating it.");
            template.execute("alter table user_settings add view_as_list boolean default false not null");
            LOG.info("Database column 'user_settings.view_as_list' was added successfully.");
        }
    }
}