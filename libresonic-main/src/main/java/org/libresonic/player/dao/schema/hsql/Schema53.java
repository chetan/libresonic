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
 *  Copyright 2015 (C) Sindre Mehus
 */

package org.libresonic.player.dao.schema.hsql;

import org.libresonic.player.Logger;
import org.libresonic.player.dao.schema.Schema;
import org.libresonic.player.domain.AlbumListType;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Used for creating and evolving the database schema.
 * This class implements the database schema for Libresonic version 5.3.
 *
 * @author Sindre Mehus
 */
public class Schema53 extends Schema {

    private static final Logger LOG = Logger.getLogger(Schema53.class);

    @Override
    public void execute(JdbcTemplate template) {

        if (template.queryForObject("select count(*) from version where version = 25", Integer.class) == 0) {
            LOG.info("Updating database schema to version 25.");
            template.execute("insert into version values (25)");
        }

        if (!rowExists(template, "table_name='PODCAST_EPISODE' and column_name='URL' and ordinal_position=1",
                       "information_schema.system_indexinfo")) {
            template.execute("create index idx_podcast_episode_url on podcast_episode(url)");
            LOG.info("Created index for podcast_episode.url");
        }

        if (!columnExists(template, "default_album_list", "user_settings")) {
            LOG.info("Database column 'user_settings.default_album_list' not found.  Creating it.");
            template.execute("alter table user_settings add default_album_list varchar default '" +
                             AlbumListType.RANDOM.getId() + "' not null");
            LOG.info("Database column 'user_settings.default_album_list' was added successfully.");
        }

        if (!columnExists(template, "queue_following_songs", "user_settings")) {
            LOG.info("Database column 'user_settings.queue_following_songs' not found.  Creating it.");
            template.execute("alter table user_settings add queue_following_songs boolean default true not null");
            LOG.info("Database column 'user_settings.queue_following_songs' was added successfully.");
        }

        if (!columnExists(template, "image_url", "podcast_channel")) {
            LOG.info("Database column 'podcast_channel.image_url' not found.  Creating it.");
            template.execute("alter table podcast_channel add image_url varchar");
            LOG.info("Database column 'podcast_channel.image_url' was added successfully.");
        }

        if (!columnExists(template, "show_side_bar", "user_settings")) {
            LOG.info("Database column 'user_settings.show_side_bar' not found.  Creating it.");
            template.execute("alter table user_settings add show_side_bar boolean default true not null");
            LOG.info("Database column 'user_settings.show_side_bar' was added successfully.");
        }

        if (!columnExists(template, "folder_id", "artist")) {
            LOG.info("Database column 'artist.folder_id' not found.  Creating it.");
            template.execute("alter table artist add folder_id int");
            LOG.info("Database column 'artist.folder_id' was added successfully.");
        }
    }
}