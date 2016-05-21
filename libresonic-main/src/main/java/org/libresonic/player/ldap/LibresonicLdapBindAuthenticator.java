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
package org.libresonic.player.ldap;

import java.util.HashMap;
import java.util.Map;

import org.acegisecurity.BadCredentialsException;
import org.acegisecurity.ldap.DefaultInitialDirContextFactory;
import org.acegisecurity.ldap.search.FilterBasedLdapUserSearch;
import org.acegisecurity.providers.ldap.LdapAuthenticator;
import org.acegisecurity.providers.ldap.authenticator.BindAuthenticator;
import org.acegisecurity.userdetails.ldap.LdapUserDetails;
import org.apache.commons.lang.StringUtils;
import org.libresonic.player.domain.User;
import org.libresonic.player.service.SecurityService;
import org.libresonic.player.service.SettingsService;

/**
 * LDAP authenticator which uses a delegate {@link BindAuthenticator}, and which
 * supports dynamically changing LDAP provider URL and search filter.
 *
 * @author Sindre Mehus
 */
public class LibresonicLdapBindAuthenticator implements LdapAuthenticator {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(LibresonicLdapBindAuthenticator.class);

    private SecurityService securityService;
    private SettingsService settingsService;

    private long authenticatorTimestamp;
    private BindAuthenticator delegateAuthenticator;

    @Override
    public LdapUserDetails authenticate(String username, String password) {

        // LDAP authentication must be enabled on the system.
        if (!settingsService.isLdapEnabled()) {
            throw new BadCredentialsException("LDAP authentication disabled.");
        }

        // User must be defined in Libresonic, unless auto-shadowing is enabled.
        User user = securityService.getUserByName(username);
        if (user == null && !settingsService.isLdapAutoShadowing()) {
            throw new BadCredentialsException("User does not exist.");
        }

        // LDAP authentication must be enabled for the given user.
        if (user != null && !user.isLdapAuthenticated()) {
            throw new BadCredentialsException("LDAP authentication disabled for user.");
        }

        try {
            createDelegate();
            LdapUserDetails details = delegateAuthenticator.authenticate(username, password);
            if (details != null) {
                LOG.info("User '" + username + "' successfully authenticated in LDAP. DN: " + details.getDn());

                if (user == null) {
                    User newUser = new User(username, "", null, true, 0L, 0L, 0L);
                    newUser.setStreamRole(true);
                    newUser.setSettingsRole(true);
                    securityService.createUser(newUser);
                    LOG.info("Created local user '" + username + "' for DN " + details.getDn());
                }
            }

            return details;
        } catch (RuntimeException x) {
            LOG.info("Failed to authenticate user '" + username + "' in LDAP.", x);
            throw x;
        }
    }

    /**
     * Creates the delegate {@link BindAuthenticator}.
     */
    private synchronized void createDelegate() {

        // Only create it if necessary.
        if (delegateAuthenticator == null || authenticatorTimestamp < settingsService.getSettingsChanged()) {

            DefaultInitialDirContextFactory contextFactory = new DefaultInitialDirContextFactory(settingsService.getLdapUrl());

            String managerDn = settingsService.getLdapManagerDn();
            String managerPassword = settingsService.getLdapManagerPassword();
            if (StringUtils.isNotEmpty(managerDn) && StringUtils.isNotEmpty(managerPassword)) {
                contextFactory.setManagerDn(managerDn);
                contextFactory.setManagerPassword(managerPassword);
            }

            Map<String, String> extraEnvVars = new HashMap<String, String>();
            extraEnvVars.put("java.naming.referral", "follow");
            contextFactory.setExtraEnvVars(extraEnvVars);

            FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch("", settingsService.getLdapSearchFilter(), contextFactory);
            userSearch.setSearchSubtree(true);
            userSearch.setDerefLinkFlag(true);

            delegateAuthenticator = new BindAuthenticator(contextFactory);
            delegateAuthenticator.setUserSearch(userSearch);

            authenticatorTimestamp = settingsService.getSettingsChanged();
        }
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }
}
