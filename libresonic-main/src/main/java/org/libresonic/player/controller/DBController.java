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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.libresonic.player.dao.DaoHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Controller for the DB admin page.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("db")
public class DBController {

    @Autowired
    private DaoHelper daoHelper;

    @RequestMapping(method = RequestMethod.GET)
    public String doGet(HttpServletRequest request, ModelMap model) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();

        String query = request.getParameter("query");
        if (query != null) {
            map.put("query", query);

            try {
                List<?> result = daoHelper.getJdbcTemplate().query(query, new ColumnMapRowMapper());
                map.put("result", result);
            } catch (DataAccessException x) {
                map.put("error", ExceptionUtils.getRootCause(x).getMessage());
            }
        }

        model.put("model", map);
        return "db";
    }

}
