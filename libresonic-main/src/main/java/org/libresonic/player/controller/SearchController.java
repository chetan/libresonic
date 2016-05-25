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
package org.libresonic.player.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.libresonic.player.command.SearchCommand;
import org.libresonic.player.domain.MusicFolder;
import org.libresonic.player.domain.SearchCriteria;
import org.libresonic.player.domain.SearchResult;
import org.libresonic.player.domain.User;
import org.libresonic.player.domain.UserSettings;
import org.libresonic.player.service.PlayerService;
import org.libresonic.player.service.SearchService;
import org.libresonic.player.service.SecurityService;
import org.libresonic.player.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for the search page.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/search.view")
public class SearchController {

    private static final int MATCH_COUNT = 25;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private SearchService searchService;

    @RequestMapping(method = RequestMethod.GET)
    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        return new SearchCommand();
    }

    @RequestMapping(method = RequestMethod.POST)
    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, SearchCommand command, BindException errors)
            throws Exception {

        User user = securityService.getCurrentUser(request);
        UserSettings userSettings = settingsService.getUserSettings(user.getUsername());
        command.setUser(user);
        command.setPartyModeEnabled(userSettings.isPartyModeEnabled());

        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(user.getUsername());
        String query = StringUtils.trimToNull(command.getQuery());

        if (query != null) {

            SearchCriteria criteria = new SearchCriteria();
            criteria.setCount(MATCH_COUNT);
            criteria.setQuery(query);

            SearchResult artists = searchService.search(criteria, musicFolders, SearchService.IndexType.ARTIST);
            command.setArtists(artists.getMediaFiles());

            SearchResult albums = searchService.search(criteria, musicFolders, SearchService.IndexType.ALBUM);
            command.setAlbums(albums.getMediaFiles());

            SearchResult songs = searchService.search(criteria, musicFolders, SearchService.IndexType.SONG);
            command.setSongs(songs.getMediaFiles());

            command.setPlayer(playerService.getPlayer(request, response));
        }

        return new ModelAndView("search", errors.getModel());
    }

}
