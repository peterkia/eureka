package edu.emory.cci.aiw.cvrg.eureka.etl.dao;

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
import com.google.inject.Inject;
import com.google.inject.Provider;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.CohortDestinationEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.DestinationEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.DestinationEntity_;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.I2B2DestinationEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.PatientSetExtractorDestinationEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.PatientSetSenderDestinationEntity;
import edu.emory.cci.aiw.cvrg.eureka.etl.entity.TabularFileDestinationEntity;
import java.util.List;
import javax.persistence.EntityManager;
import org.eurekaclinical.standardapis.dao.HistoricalGenericDao;

/**
 *
 * @author Andrew Post
 */
public class JpaDestinationDao extends HistoricalGenericDao<DestinationEntity, Long> implements DestinationDao {

	@Inject
	public JpaDestinationDao(Provider<EntityManager> inManagerProvider) {
		super(DestinationEntity.class, inManagerProvider);
	}

	@Override
	public DestinationEntity getCurrentByName(String name) {
		return getDatabaseSupport().getCurrentUniqueByAttribute(
				DestinationEntity.class,
				DestinationEntity_.name, name);
	}

	@Override
	public List<CohortDestinationEntity> getCurrentCohortDestinations() {
		return getDatabaseSupport().getCurrent(CohortDestinationEntity.class);
	}

	@Override
	public List<I2B2DestinationEntity> getCurrentI2B2Destinations() {
		return getDatabaseSupport().getCurrent(I2B2DestinationEntity.class);
	}

	@Override
	public List<PatientSetExtractorDestinationEntity> getCurrentPatientSetExtractorDestinations() {
		return getDatabaseSupport().getCurrent(PatientSetExtractorDestinationEntity.class);
	}

	@Override
	public List<PatientSetSenderDestinationEntity> getCurrentPatientSetSenderDestinations() {
		return getDatabaseSupport().getCurrent(PatientSetSenderDestinationEntity.class);
	}

	@Override
	public List<TabularFileDestinationEntity> getCurrentTabularFileDestinations() {
		return getDatabaseSupport().getCurrent(TabularFileDestinationEntity.class);
	}

}
