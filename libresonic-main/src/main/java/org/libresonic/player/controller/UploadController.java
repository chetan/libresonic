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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.libresonic.player.domain.TransferStatus;
import org.libresonic.player.domain.User;
import org.libresonic.player.service.PlayerService;
import org.libresonic.player.service.SecurityService;
import org.libresonic.player.service.SettingsService;
import org.libresonic.player.service.StatusService;
import org.libresonic.player.upload.MonitoredDiskFileItemFactory;
import org.libresonic.player.upload.UploadListener;
import org.libresonic.player.util.StringUtil;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 * Controller which receives uploaded files.
 *
 * @author Sindre Mehus
 */
public class UploadController extends ParameterizableViewController {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(UploadController.class);

    private SecurityService securityService;
    private PlayerService playerService;
    private StatusService statusService;
    private SettingsService settingsService;
    public static final String UPLOAD_STATUS = "uploadStatus";

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Map<String, Object> map = new HashMap<String, Object>();
        List<File> uploadedFiles = new ArrayList<File>();
        List<File> unzippedFiles = new ArrayList<File>();
        TransferStatus status = null;

        try {

            status = statusService.createUploadStatus(playerService.getPlayer(request, response, false, false));
            status.setBytesTotal(request.getContentLength());

            request.getSession().setAttribute(UPLOAD_STATUS, status);

            // Check that we have a file upload request
            if (!ServletFileUpload.isMultipartContent(request)) {
                throw new Exception("Illegal request.");
            }

            File dir = null;
            boolean unzip = false;

            UploadListener listener = new UploadListenerImpl(status);

            FileItemFactory factory = new MonitoredDiskFileItemFactory(listener);
            ServletFileUpload upload = new ServletFileUpload(factory);

            List<?> items = upload.parseRequest(request);

            // First, look for "dir" and "unzip" parameters.
            for (Object o : items) {
                FileItem item = (FileItem) o;

                if (item.isFormField() && "dir".equals(item.getFieldName())) {
                    dir = new File(item.getString());
                } else if (item.isFormField() && "unzip".equals(item.getFieldName())) {
                    unzip = true;
                }
            }

            if (dir == null) {
                throw new Exception("Missing 'dir' parameter.");
            }

            // Look for file items.
            for (Object o : items) {
                FileItem item = (FileItem) o;

                if (!item.isFormField()) {
                    String fileName = item.getName();
                    if (fileName.trim().length() > 0) {

                        File targetFile = new File(dir, new File(fileName).getName());

                        if (!securityService.isUploadAllowed(targetFile)) {
                            throw new Exception("Permission denied: " + StringUtil.toHtml(targetFile.getPath()));
                        }

                        if (!dir.exists()) {
                            dir.mkdirs();
                        }

                        item.write(targetFile);
                        uploadedFiles.add(targetFile);
                        LOG.info("Uploaded " + targetFile);

                        if (unzip && targetFile.getName().toLowerCase().endsWith(".zip")) {
                            unzip(targetFile, unzippedFiles);
                        }
                    }
                }
            }

        } catch (Exception x) {
            LOG.warn("Uploading failed.", x);
            map.put("exception", x);
        } finally {
            if (status != null) {
                statusService.removeUploadStatus(status);
                request.getSession().removeAttribute(UPLOAD_STATUS);
                User user = securityService.getCurrentUser(request);
                securityService.updateUserByteCounts(user, 0L, 0L, status.getBytesTransfered());
            }
        }

        map.put("uploadedFiles", uploadedFiles);
        map.put("unzippedFiles", unzippedFiles);

        ModelAndView result = super.handleRequestInternal(request, response);
        result.addObject("model", map);
        return result;
    }

    private void unzip(File file, List<File> unzippedFiles) throws Exception {
        LOG.info("Unzipping " + file);

        ZipFile zipFile = new ZipFile(file);

        try {

            Enumeration<?> entries = zipFile.getEntries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                File entryFile = new File(file.getParentFile(), entry.getName());

                if (!entry.isDirectory()) {

                    if (!securityService.isUploadAllowed(entryFile)) {
                        throw new Exception("Permission denied: " + StringUtil.toHtml(entryFile.getPath()));
                    }

                    entryFile.getParentFile().mkdirs();
                    InputStream inputStream = null;
                    OutputStream outputStream = null;
                    try {
                        inputStream = zipFile.getInputStream(entry);
                        outputStream = new FileOutputStream(entryFile);

                        byte[] buf = new byte[8192];
                        while (true) {
                            int n = inputStream.read(buf);
                            if (n == -1) {
                                break;
                            }
                            outputStream.write(buf, 0, n);
                        }

                        LOG.info("Unzipped " + entryFile);
                        unzippedFiles.add(entryFile);
                    } finally {
                        IOUtils.closeQuietly(inputStream);
                        IOUtils.closeQuietly(outputStream);
                    }
                }
            }

            zipFile.close();
            file.delete();

        } finally {
            zipFile.close();
        }
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setStatusService(StatusService statusService) {
        this.statusService = statusService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * Receives callbacks as the file upload progresses.
     */
    private class UploadListenerImpl implements UploadListener {
        private final TransferStatus status;
        private final long start;

        private UploadListenerImpl(TransferStatus status) {
            this.status = status;
            start = System.currentTimeMillis();
        }

        @Override
        public void start(String fileName) {
            status.setFile(new File(fileName));
        }

        @Override
        public void bytesRead(long bytesRead) {

            // Throttle bitrate.

            long byteCount = status.getBytesTransfered() + bytesRead;
            long bitCount = byteCount * 8L;

            float elapsedMillis = Math.max(1, System.currentTimeMillis() - start);
            float elapsedSeconds = elapsedMillis / 1000.0F;
            long maxBitsPerSecond = getBitrateLimit();

            status.setBytesTransfered(byteCount);

            if (maxBitsPerSecond > 0) {
                float sleepMillis = 1000.0F * (bitCount / maxBitsPerSecond - elapsedSeconds);
                if (sleepMillis > 0) {
                    try {
                        Thread.sleep((long) sleepMillis);
                    } catch (InterruptedException x) {
                        LOG.warn("Failed to sleep.", x);
                    }
                }
            }
        }

        private long getBitrateLimit() {
            return 1024L * settingsService.getUploadBitrateLimit() / Math.max(1, statusService.getAllUploadStatuses().size());
        }
    }

}