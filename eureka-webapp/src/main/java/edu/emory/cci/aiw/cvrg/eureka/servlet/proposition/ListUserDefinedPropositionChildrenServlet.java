/*
 * #%L
 * Eureka WebApp
 * %%
 * Copyright (C) 2012 Emory University
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package edu.emory.cci.aiw.cvrg.eureka.servlet.proposition;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.emory.cci.aiw.cvrg.eureka.common.comm.CategoricalElement;
import edu.emory.cci.aiw.cvrg.eureka.common.comm.DataElement;
import edu.emory.cci.aiw.cvrg.eureka.common.comm.clients.ServicesClient;
import edu.emory.cci.aiw.cvrg.eureka.common.entity.User;

public class ListUserDefinedPropositionChildrenServlet extends HttpServlet {

	private static final Logger LOGGER = LoggerFactory
	        .getLogger(ListUserDefinedPropositionChildrenServlet.class);
	private ServicesClient servicesClient;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		String parameter = config.getServletContext().getInitParameter
				("eureka-services-url");
		this.servicesClient = new ServicesClient(parameter);
	}

	private String getDisplayName(DataElement p) {
		String displayName = "";

		if (p.getAbbrevDisplayName() != null
		        && !p.getAbbrevDisplayName().equals("")) {

			displayName = p.getAbbrevDisplayName();

		} else if (p.getDisplayName() != null && !p.getDisplayName().equals("")) {

			displayName = p.getDisplayName();

		} else {

			displayName = p.getKey();

		}

		return displayName;
	}


	private JsonTreeData createData(String data, String key) {
		JsonTreeData d = new JsonTreeData();
		d.setData(data);
		d.setKeyVal("key", key);
		d.setKeyVal("data-key", key);

		return d;
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	        throws ServletException, IOException {
		doGet(req, resp);
	}

	private void getAllData(Long inUserId, JsonTreeData d) {
		DataElement dataElement = this.servicesClient.getUserElement
				(inUserId, d.getAttr().get("data-key"));

		if (dataElement.getType() == DataElement.Type.CATEGORIZATION) {
			CategoricalElement ce = (CategoricalElement) dataElement;
			for (DataElement de : ce.getChildren()) {
				if (de.isInSystem()) {

					JsonTreeData newData = createData(this.getDisplayName
							(de),de.getKey());
					newData.setType("system");
					LOGGER.debug("add sysTarget {}", de.getKey());
					d.addNodes(newData);

				}
			}

			for (DataElement userDataElement : ce.getChildren()) {
				if (!userDataElement.isInSystem()) {

					JsonTreeData newData = createData(
					        userDataElement.getAbbrevDisplayName(),
							userDataElement.getKey());
					getAllData(inUserId, newData);
					newData.setType("user");
					LOGGER.debug("add user defined {}", userDataElement.getKey
							());
					d.addNodes(newData);
				}
			}
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	        throws ServletException, IOException {

		List<JsonTreeData> l = new ArrayList<JsonTreeData>();
		String propKey = req.getParameter("propKey");

		Principal principal = req.getUserPrincipal();
		String userName = principal.getName();
		User user = this.servicesClient.getUserByName(userName);
		DataElement dataElement = servicesClient.getUserElement(user.getId(),
				propKey);

		JsonTreeData newData = createData(this.getDisplayName(dataElement),
		        propKey);
		getAllData(user.getId(),newData);
		l.add(newData);

		ObjectMapper mapper = new ObjectMapper();
		resp.setContentType("application/json");
		PrintWriter out = resp.getWriter();
		mapper.writeValue(out, l);
	}
}
