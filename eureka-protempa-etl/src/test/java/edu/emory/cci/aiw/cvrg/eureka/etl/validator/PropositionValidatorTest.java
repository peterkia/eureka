/*
 * #%L
 * Eureka Protempa ETL
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
package edu.emory.cci.aiw.cvrg.eureka.etl.validator;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.protempa.EventDefinition;
import org.protempa.PropositionDefinition;

import com.google.inject.Module;
import com.google.inject.persist.PersistService;

import edu.emory.cci.aiw.cvrg.eureka.common.test.AbstractTest;
import edu.emory.cci.aiw.cvrg.eureka.common.test.TestDataException;
import edu.emory.cci.aiw.cvrg.eureka.common.test.TestDataProvider;
import edu.emory.cci.aiw.cvrg.eureka.etl.config.AppTestModule;
import edu.emory.cci.aiw.cvrg.eureka.etl.dao.ConfDao;
import edu.emory.cci.aiw.cvrg.eureka.etl.test.Setup;
import edu.stanford.smi.protege.util.Assert;
import org.junit.After;

import static org.junit.Assert.assertTrue;
import org.junit.Before;

public class PropositionValidatorTest extends AbstractTest {

	private static Long USER_ID = Long.valueOf(1L);
	
	/**
	 * Instance of a class that can set up and tear down test data.
	 */
	private TestDataProvider testDataProvider;
	/**
	 * Instance of a persistence service, to be used to store test data and run
	 * queries against.
	 */
	private PersistService persistService;
	
	@Before
	public void beforePropositionValidatorTest() throws TestDataException {
		persistService = getInstance(PersistService.class);
		persistService.start();
		testDataProvider = getInstance(getDataProvider());
		testDataProvider.setUp();
	}
	
	@After
	public void afterPropositionValidatorTest() throws TestDataException {
		testDataProvider.tearDown();
		persistService.stop();
		testDataProvider = null;
		persistService = null;
	}
	
	protected Class<? extends TestDataProvider> getDataProvider() {
		return Setup.class;
	}

	@Override
	protected Module[] getModules() {
		return new Module[] {new AppTestModule()};
	}

	@Test
	public void testNoPropositions() {
		ConfDao confDao = this.getInstance(ConfDao.class);
		PropositionValidator validator = this.getInstance
			(PropositionValidator.class);
		List<PropositionDefinition> wrappers = new
			ArrayList<PropositionDefinition>();
		validator.setUserPropositions(wrappers);
		validator.setConfiguration(confDao.getByUserId(USER_ID));

		boolean actual;
		try {
			actual = validator.validate();
		} catch (PropositionValidatorException e) {
			e.printStackTrace();
			actual = false;
		}

		assertTrue(actual);
	}

	@Test
	public void testSinglePropositionNoDef() {
		ConfDao confDao = this.getInstance(ConfDao.class);
		PropositionValidator validator = this.getInstance(PropositionValidator.class);
		List<PropositionDefinition> definitions = new
			ArrayList<PropositionDefinition>();
		EventDefinition event = new EventDefinition("TestEvent");
		definitions.add(event);
		validator.setConfiguration(confDao.getByUserId(USER_ID));
		validator.setUserPropositions(definitions);

		boolean actual;
		try {
			actual = validator.validate();
			if (! validator.getMessages().isEmpty()) {
				for (String message : validator.getMessages()) {
					System.out.println(message);
				}
			}
		} catch (PropositionValidatorException e) {
			e.printStackTrace();
			actual = false;
		}
		assertTrue(actual);
	}

	@Test
	public void testCycleDetection() throws PropositionValidatorException {

		ConfDao confDao = this.getInstance(ConfDao.class);
		PropositionValidator validator = this.getInstance
			(PropositionValidator.class);

		EventDefinition def1 = new EventDefinition("TestEvent1");
		EventDefinition def2 = new EventDefinition("TestEvent2");
		EventDefinition def3 = new EventDefinition("TestEvent3");

		def1.setInverseIsA(def2.getId());
		def2.setInverseIsA(def1.getId());
		def3.setInverseIsA(def1.getId());

		List<PropositionDefinition> propositions = new ArrayList
			<PropositionDefinition>();
		propositions.add(def1);
		propositions.add(def2);

		validator.setConfiguration(confDao.getByUserId(USER_ID));
		validator.setUserPropositions(propositions);
		validator.setTargetProposition(def3);
		boolean result;
		result = validator.validate();
	
		Assert.assertFalse(result);
	}
	
	

}
