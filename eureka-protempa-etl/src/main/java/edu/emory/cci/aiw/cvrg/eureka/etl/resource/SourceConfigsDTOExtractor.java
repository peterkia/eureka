/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.emory.cci.aiw.cvrg.eureka.etl.resource;

/*
 * #%L
 * Eureka Protempa ETL
 * %%
 * Copyright (C) 2012 - 2014 Emory University
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

import edu.emory.cci.aiw.cvrg.eureka.common.comm.SourceConfig;
import edu.emory.cci.aiw.cvrg.eureka.common.entity.EtlUserEntity;
import edu.emory.cci.aiw.cvrg.eureka.common.entity.SourceConfigEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.config.EtlProperties;
import edu.emory.cci.aiw.cvrg.eureka.etl.config.EurekaProtempaConfigurations;
import edu.emory.cci.aiw.cvrg.eureka.etl.dao.EtlGroupDao;
import edu.emory.cci.aiw.cvrg.eureka.etl.dao.ResolvedPermissions;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.protempa.backend.Backend;
import org.protempa.backend.BackendInstanceSpec;
import org.protempa.backend.BackendPropertySpec;
import org.protempa.backend.BackendProvider;
import org.protempa.backend.BackendProviderManager;
import org.protempa.backend.BackendProviderSpecLoaderException;
import org.protempa.backend.BackendSpec;
import org.protempa.backend.BackendSpecLoader;
import org.protempa.backend.ConfigurationsLoadException;
import org.protempa.backend.ConfigurationsNotFoundException;
import org.protempa.backend.InvalidPropertyNameException;
import org.protempa.backend.asb.AlgorithmSourceBackend;
import org.protempa.backend.dsb.DataSourceBackend;
import org.protempa.backend.ksb.KnowledgeSourceBackend;
import org.protempa.backend.tsb.TermSourceBackend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Andrew Post
 */
public class SourceConfigsDTOExtractor extends ConfigsDTOExtractor<SourceConfig, SourceConfigEntity> {
	private static final Logger LOGGER = LoggerFactory.getLogger(SourceConfigsDTOExtractor.class);
	
	private final EtlGroupDao groupDao;
	private final EtlProperties etlProperties;
	private final EurekaProtempaConfigurations configs;
	private static final BackendSpecLoader<DataSourceBackend> dataSourceBackendSpecLoader;
	private static final BackendSpecLoader<KnowledgeSourceBackend> knowledgeSourceBackendSpecLoader;
	private static final BackendSpecLoader<AlgorithmSourceBackend> algorithmSourceBackendSpecLoader;
	private static final BackendSpecLoader<TermSourceBackend> termSourceBackendSpecLoader;
	
	static {
		BackendProvider backendProvider =
					BackendProviderManager.getBackendProvider();
		try {
			dataSourceBackendSpecLoader =
					backendProvider.getDataSourceBackendSpecLoader();
			knowledgeSourceBackendSpecLoader =
					backendProvider.getKnowledgeSourceBackendSpecLoader();
			algorithmSourceBackendSpecLoader =
					backendProvider.getAlgorithmSourceBackendSpecLoader();
			termSourceBackendSpecLoader =
					backendProvider.getTermSourceBackendSpecLoader();
		} catch (BackendProviderSpecLoaderException ex) {
			throw new AssertionError("Problem loading backend implementions classes", ex);
		}
	}

	public SourceConfigsDTOExtractor(EtlUserEntity user, EtlGroupDao inGroupDao, EtlProperties inEtlProperties) {
		super(user);
		this.groupDao = inGroupDao;
		this.etlProperties = inEtlProperties;
		this.configs = new EurekaProtempaConfigurations(this.etlProperties);
	}
	
	@Override
	SourceConfig extractDTO(Perm perm, SourceConfigEntity configEntity) {
		String configId = configEntity.getName();
		try {
			SourceConfig config = new SourceConfig();

			List<String> badConfigIds = new ArrayList<>();
			for (String id : configs.loadConfigurationIds(configId)) {
				if (!(dataSourceBackendSpecLoader != null && dataSourceBackendSpecLoader.hasSpec(id))
						&& !(knowledgeSourceBackendSpecLoader != null && knowledgeSourceBackendSpecLoader.hasSpec(id))
						&& !(algorithmSourceBackendSpecLoader != null && algorithmSourceBackendSpecLoader.hasSpec(id))
						&& !(termSourceBackendSpecLoader != null && termSourceBackendSpecLoader.hasSpec(id))) {
					badConfigIds.add(id);
				}
			}
			if (!badConfigIds.isEmpty()) {
				if (badConfigIds.size() == 1) {
					throw new ConfigurationsLoadException("Invalid section id '" + badConfigIds.get(0) + "' in source configuration '" + configId + "'");
				} else {
					throw new ConfigurationsLoadException("Invalid section ids '" + StringUtils.join(badConfigIds.subList(0, badConfigIds.size() - 1), "', '") + "' and '" + badConfigIds.get(badConfigIds.size() - 1) + "' in source configuration '" + configId + "'");
				}
			}

			config.setId(configId);

			config.setRead(perm.read);
			config.setWrite(perm.write);
			config.setExecute(perm.execute);
			config.setOwnerUsername(perm.owner.getUsername());

			List<SourceConfig.Section> dataSourceBackendSections =
					extractConfig(dataSourceBackendSpecLoader, configs, configId);
			config.setDataSourceBackends(
					dataSourceBackendSections.toArray(
					new SourceConfig.Section[dataSourceBackendSections.size()]));


			List<SourceConfig.Section> knowledgeSourceBackendSections =
					extractConfig(knowledgeSourceBackendSpecLoader, configs, configId);
			config.setKnowledgeSourceBackends(
					knowledgeSourceBackendSections.toArray(
					new SourceConfig.Section[knowledgeSourceBackendSections.size()]));


			List<SourceConfig.Section> algorithmSourceBackendSections =
					extractConfig(algorithmSourceBackendSpecLoader, configs, configId);
			config.setAlgorithmSourceBackends(
					algorithmSourceBackendSections.toArray(
					new SourceConfig.Section[algorithmSourceBackendSections.size()]));

			List<SourceConfig.Section> termSourceBackendSections =
					extractConfig(termSourceBackendSpecLoader, configs, configId);
			config.setTermSourceBackends(
					termSourceBackendSections.toArray(
					new SourceConfig.Section[termSourceBackendSections.size()]));
			return config;
		} catch (ConfigurationsNotFoundException | ConfigurationsLoadException | InvalidPropertyNameException ex) {
			LOGGER.warn("Error getting INI file for source config {}. This source config will be ignored.", configEntity.getName(), ex);
			return null;
		}
	}

	@Override
	ResolvedPermissions resolvePermissions(EtlUserEntity owner, SourceConfigEntity entity) {
		return this.groupDao.resolveSourceConfigPermissions(owner, entity);
	}

	private <B extends Backend> List<SourceConfig.Section> extractConfig(
			BackendSpecLoader<B> backendSpecLoader,
			EurekaProtempaConfigurations configs, String sourceId)
			throws ConfigurationsNotFoundException,
			ConfigurationsLoadException, InvalidPropertyNameException {
		List<SourceConfig.Section> backendSections = new ArrayList<>();
		if (backendSpecLoader != null) {
			for (BackendSpec<B> bs : backendSpecLoader) {
				List<BackendInstanceSpec<B>> load = configs.load(sourceId, bs);
				for (BackendInstanceSpec<B> bis : load) {
					SourceConfig.Section section = new SourceConfig.Section();
					section.setId(bs.getId());
					section.setDisplayName(bs.getDisplayName());
					List<BackendPropertySpec> backendPropertySpecs = bis.getBackendPropertySpecs();
					SourceConfig.Option[] options = new SourceConfig.Option[backendPropertySpecs.size()];
					for (int i = 0; i < options.length; i++) {
						BackendPropertySpec property = backendPropertySpecs.get(i);
						Object value = bis.getProperty(property.getName());
						SourceConfig.Option option = new SourceConfig.Option();
						option.setKey(property.getName());
						option.setValue(value);
						options[i] = option;
					}
					section.setOptions(options);
					backendSections.add(section);
				}
			}
		}
		return backendSections;
	}
}
