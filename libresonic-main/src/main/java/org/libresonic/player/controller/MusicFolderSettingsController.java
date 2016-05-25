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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.libresonic.player.command.MusicFolderSettingsCommand;
import org.libresonic.player.dao.AlbumDao;
import org.libresonic.player.dao.ArtistDao;
import org.libresonic.player.dao.MediaFileDao;
import org.libresonic.player.domain.MusicFolder;
import org.libresonic.player.service.MediaScannerService;
import org.libresonic.player.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for the page used to administrate the set of music folders.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/musicFolderSettings")
public class MusicFolderSettingsController {

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private MediaScannerService mediaScannerService;

    @Autowired
    private ArtistDao artistDao;

    @Autowired
    private AlbumDao albumDao;

    @Autowired
    private MediaFileDao mediaFileDao;

    @RequestMapping(method = RequestMethod.GET)
    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        MusicFolderSettingsCommand command = new MusicFolderSettingsCommand();

        if (request.getParameter("scanNow") != null) {
            settingsService.clearMusicFolderCache();
            mediaScannerService.scanLibrary();
        }
        if (request.getParameter("expunge") != null) {
            expunge();
        }

        command.setInterval(String.valueOf(settingsService.getIndexCreationInterval()));
        command.setHour(String.valueOf(settingsService.getIndexCreationHour()));
        command.setFastCache(settingsService.isFastCacheEnabled());
        command.setOrganizeByFolderStructure(settingsService.isOrganizeByFolderStructure());
        command.setScanning(mediaScannerService.isScanning());
        command.setMusicFolders(wrap(settingsService.getAllMusicFolders(true, true)));
        command.setNewMusicFolder(new MusicFolderSettingsCommand.MusicFolderInfo());
        command.setReload(request.getParameter("reload") != null || request.getParameter("scanNow") != null);
        return command;
    }

    private void expunge() {
        artistDao.expunge();
        albumDao.expunge();
        mediaFileDao.expunge();
    }

    private List<MusicFolderSettingsCommand.MusicFolderInfo> wrap(List<MusicFolder> musicFolders) {
        ArrayList<MusicFolderSettingsCommand.MusicFolderInfo> result = new ArrayList<MusicFolderSettingsCommand.MusicFolderInfo>();
        for (MusicFolder musicFolder : musicFolders) {
            result.add(new MusicFolderSettingsCommand.MusicFolderInfo(musicFolder));
        }
        return result;
    }

    @RequestMapping(method = RequestMethod.POST)
    protected ModelAndView onSubmit(MusicFolderSettingsCommand command) throws Exception {

        for (MusicFolderSettingsCommand.MusicFolderInfo musicFolderInfo : command.getMusicFolders()) {
            if (musicFolderInfo.isDelete()) {
                settingsService.deleteMusicFolder(musicFolderInfo.getId());
            } else {
                MusicFolder musicFolder = musicFolderInfo.toMusicFolder();
                if (musicFolder != null) {
                    settingsService.updateMusicFolder(musicFolder);
                }
            }
        }

        MusicFolder newMusicFolder = command.getNewMusicFolder().toMusicFolder();
        if (newMusicFolder != null) {
            settingsService.createMusicFolder(newMusicFolder);
        }

        settingsService.setIndexCreationInterval(Integer.parseInt(command.getInterval()));
        settingsService.setIndexCreationHour(Integer.parseInt(command.getHour()));
        settingsService.setFastCacheEnabled(command.isFastCache());
        settingsService.setOrganizeByFolderStructure(command.isOrganizeByFolderStructure());
        settingsService.save();

        mediaScannerService.schedule();
        return new ModelAndView(new RedirectView("musicFolderSettings.view?reload"));
    }

}