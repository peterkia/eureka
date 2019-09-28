package edu.emory.cci.aiw.cvrg.eureka.etl.resource;

/*
 * #%L
 * Eureka Protempa ETL
 * %%
 * Copyright (C) 2012 - 2013 Emory University
 * %%
 * This program is dual licensed under the Apache 2 and GPLv3 licenses.
 * 
 * Apache License, Version 2.0:
 * 
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
 * 
 * GNU General Public License version 3:
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import org.eurekaclinical.protempa.client.comm.EtlCohortDestination;
import org.eurekaclinical.protempa.client.comm.EtlDestination;
import org.eurekaclinical.protempa.client.comm.EtlI2B2Destination;
import org.eurekaclinical.protempa.client.comm.EtlPatientSetExtractorDestination;
import org.eurekaclinical.protempa.client.comm.EtlPatientSetSenderDestination;
import org.eurekaclinical.protempa.client.comm.EtlTabularFileDestination;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.CohortDestinationEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.DestinationEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.DestinationGroupMembership;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.EtlGroup;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.AuthorizedUserEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.I2B2DestinationEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.PatientSetExtractorDestinationEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.PatientSetSenderDestinationEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.TabularFileDestinationEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.config.EtlProperties;
import edu.emory.cci.aiw.cvrg.eureka.etl.dao.DestinationDao;
import edu.emory.cci.aiw.cvrg.eureka.etl.dao.EtlGroupDao;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ws.rs.core.Response;
import org.eurekaclinical.standardapis.exception.HttpStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Andrew Post
 */
public final class Destinations {

	/**
	 * The class level logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(Destinations.class);

	private final EtlGroupDao groupDao;
	private final AuthorizedUserEntity etlUser;
	private final DestinationDao destinationDao;
	private final EtlProperties etlProperties;
	private final EtlDestinationToDestinationEntityVisitor destToDestEntityVisitor;

	public Destinations(EtlProperties inEtlProperties, AuthorizedUserEntity inEtlUser,
			DestinationDao inDestinationDao, EtlGroupDao inGroupDao,
			EtlDestinationToDestinationEntityVisitor inDestToDestEntityVisitor) {
		this.groupDao = inGroupDao;
		this.etlUser = inEtlUser;
		this.destinationDao = inDestinationDao;
		this.etlProperties = inEtlProperties;
		this.destToDestEntityVisitor = inDestToDestEntityVisitor;
	}

	public Long create(EtlDestination etlDestination) {
		if (etlDestination.getOwnerUserId() == null) {
			etlDestination.setOwnerUserId(this.etlUser.getId());
		}
		etlDestination.accept(this.destToDestEntityVisitor);
		DestinationEntity destinationEntity = this.destToDestEntityVisitor.getDestinationEntity();
		return this.destinationDao.create(destinationEntity).getId();
	}

	public void update(EtlDestination etlDestination) {
		DestinationEntity oldEntity = this.destinationDao.retrieve(etlDestination.getId());
		if (oldEntity == null) {
			throw new HttpStatusException(Response.Status.NOT_FOUND);
		}
		if (!this.etlUser.getId().equals(etlDestination.getOwnerUserId())) {
			throw new HttpStatusException(Response.Status.NOT_FOUND);
		}
		if (etlDestination.getOwnerUserId() == null) {
			etlDestination.setOwnerUserId(this.etlUser.getId());
		}
		
		etlDestination.accept(this.destToDestEntityVisitor);
		DestinationEntity cde = this.destToDestEntityVisitor.getDestinationEntity();
		this.destinationDao.updateCurrent(cde);
	}

	List<DestinationEntity> configs(AuthorizedUserEntity user) {
		return user.getDestinations();
	}

	List<DestinationGroupMembership> groupConfigs(EtlGroup group) {
		return group.getDestinations();
	}

	String toConfigId(File file) {
		return FromConfigFile.toDestId(file);
	}

	public List<EtlI2B2Destination> getAllI2B2s() {
		List<EtlI2B2Destination> result = new ArrayList<>();
		I2B2DestinationsDTOExtractor extractor
				= new I2B2DestinationsDTOExtractor(this.etlProperties, this.etlUser, this.groupDao);
		for (I2B2DestinationEntity configEntity
				: this.destinationDao.getCurrentI2B2Destinations()) {
			EtlI2B2Destination dto = extractor.extractDTO(configEntity);
			if (dto != null) {
				result.add(dto);
			}
		}
		return result;
	}

	public List<EtlCohortDestination> getAllCohorts() {
		List<EtlCohortDestination> result = new ArrayList<>();
		CohortDestinationsDTOExtractor extractor
				= new CohortDestinationsDTOExtractor(this.etlUser, this.groupDao);
		for (CohortDestinationEntity configEntity
				: this.destinationDao.getCurrentCohortDestinations()) {
			EtlCohortDestination dto = extractor.extractDTO(configEntity);
			if (dto != null) {
				result.add(dto);
			}
		}
		return result;
	}

	public List<EtlPatientSetExtractorDestination> getAllPatientSetExtractors() {
		List<EtlPatientSetExtractorDestination> result = new ArrayList<>();
		PatientSetExtractorDestinationsDTOExtractor extractor
				= new PatientSetExtractorDestinationsDTOExtractor(this.etlUser, this.groupDao);
		for (PatientSetExtractorDestinationEntity configEntity
				: this.destinationDao.getCurrentPatientSetExtractorDestinations()) {
			EtlPatientSetExtractorDestination dto = extractor.extractDTO(configEntity);
			if (dto != null) {
				result.add(dto);
			}
		}
		return result;
	}

	public List<EtlPatientSetSenderDestination> getAllPatientSetSenders() {
		List<EtlPatientSetSenderDestination> result = new ArrayList<>();
		PatientSetSenderDestinationsDTOExtractor extractor
				= new PatientSetSenderDestinationsDTOExtractor(this.etlUser, this.groupDao);
		for (PatientSetSenderDestinationEntity configEntity
				: this.destinationDao.getCurrentPatientSetSenderDestinations()) {
			EtlPatientSetSenderDestination dto = extractor.extractDTO(configEntity);
			if (dto != null) {
				result.add(dto);
			}
		}
		return result;
	}

	public List<EtlTabularFileDestination> getAllTabularFiles() {
		List<EtlTabularFileDestination> result = new ArrayList<>();
		TabularFileDestinationsDTOExtractor extractor
				= new TabularFileDestinationsDTOExtractor(this.etlUser, this.groupDao);
		for (TabularFileDestinationEntity configEntity
				: this.destinationDao.getCurrentTabularFileDestinations()) {
			EtlTabularFileDestination dto = extractor.extractDTO(configEntity);
			if (dto != null) {
				result.add(dto);
			}
		}
		return result;
	}

	/**
	 * Gets the specified source extractDTO. If it does not exist or the current
	 * user lacks read permissions for it, this method returns
	 * <code>null</code>.
	 *
	 * @return a extractDTO.
	 */
	public EtlDestination getOne(String configId) {
		if (configId == null) {
			throw new IllegalArgumentException("configId cannot be null");
		}
		DestinationDTOExtractorVisitor visitor
				= new DestinationDTOExtractorVisitor(this.etlProperties, this.etlUser, this.groupDao);

		DestinationEntity byName = this.destinationDao.getCurrentByName(configId);
		if (byName == null) {
			throw new HttpStatusException(Response.Status.NOT_FOUND);
		}
		byName.accept(visitor);
		return visitor.getEtlDestination();
	}

	/**
	 * Gets all configs for which the current user has read permissions.
	 *
	 * @return a {@link List} of configs.
	 */
	public final List<EtlDestination> getAll() {
		List<EtlDestination> result = new ArrayList<>();
		DestinationDTOExtractorVisitor visitor
				= new DestinationDTOExtractorVisitor(this.etlProperties, this.etlUser, this.groupDao);
		for (DestinationEntity configEntity : this.destinationDao.getCurrent()) {
			configEntity.accept(visitor);
			EtlDestination dto = visitor.getEtlDestination();
			if (dto != null) {
				result.add(dto);
			}
		}
		return result;
	}

	public void delete(String destId) {
		DestinationEntity dest = this.destinationDao.getCurrentByName(destId);
		if (dest == null || !this.etlUser.equals(dest.getOwner())) {
			throw new HttpStatusException(Response.Status.NOT_FOUND);
		}
		dest.setExpiredAt(new Date());
		this.destinationDao.update(dest);
	}

}
