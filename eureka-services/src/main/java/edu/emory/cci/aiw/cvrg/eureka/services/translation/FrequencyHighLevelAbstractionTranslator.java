/*
 * #%L
 * Eureka Services
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
package edu.emory.cci.aiw.cvrg.eureka.services.translation;

import com.google.inject.Inject;
import edu.emory.cci.aiw.cvrg.eureka.common.comm.DataElementField;
import edu.emory.cci.aiw.cvrg.eureka.common.comm.Frequency;
import edu.emory.cci.aiw.cvrg.eureka.common.entity.Categorization;
import edu.emory.cci.aiw.cvrg.eureka.common.entity.CompoundLowLevelAbstraction;
import edu.emory.cci.aiw.cvrg.eureka.common.entity.ExtendedProposition;
import edu.emory.cci.aiw.cvrg.eureka.common.entity.HighLevelAbstraction;
import edu.emory.cci.aiw.cvrg.eureka.common.entity.LowLevelAbstraction;
import edu.emory.cci.aiw.cvrg.eureka.common.entity.Proposition;
import edu.emory.cci.aiw.cvrg.eureka.common.entity.PropositionEntityVisitor;
import edu.emory.cci.aiw.cvrg.eureka.common.entity.Relation;
import edu.emory.cci.aiw.cvrg.eureka.common.entity.SliceAbstraction;
import edu.emory.cci.aiw.cvrg.eureka.common.entity.SystemProposition;
import edu.emory.cci.aiw.cvrg.eureka.common.exception.DataElementHandlingException;
import edu.emory.cci.aiw.cvrg.eureka.services.dao.PropositionDao;
import edu.emory.cci.aiw.cvrg.eureka.services.dao.TimeUnitDao;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

public final class FrequencyHighLevelAbstractionTranslator implements
		PropositionTranslator<Frequency, HighLevelAbstraction> {

	private static final String INTERMEDIATE_SUFFIX = "_FREQUENCY";

	private final PropositionDao propositionDao;
	private final TimeUnitDao timeUnitDao;
	private Long userId;

	@Inject
	public FrequencyHighLevelAbstractionTranslator(
			PropositionDao inPropositionDao, TimeUnitDao inTimeUnitDao) {
		propositionDao = inPropositionDao;
		timeUnitDao = inTimeUnitDao;
	}

	public void setUserId(Long inUserId) {
		this.userId = inUserId;
	}

	@Override
	public HighLevelAbstraction translateFromElement(Frequency element)
			throws DataElementHandlingException {
		HighLevelAbstraction result = new HighLevelAbstraction();
		PropositionTranslatorUtil.populateCommonPropositionFields(result,
				element);
		result.setCreatedFrom(HighLevelAbstraction.CreatedFrom.FREQUENCY);

		Proposition abstractedFrom = propositionDao.getByUserAndKey(userId,
				element.getDataElement().getDataElementKey());

		IntermediateAbstractionGenerator iag = new
				IntermediateAbstractionGenerator(element);
		abstractedFrom.accept(iag);
		result.setRelations(Collections.singletonList(relationFromEP
				(epFromAbstraction(iag.getIntermediateAbstraction(), element))));
		result.setAbstractedFrom(Collections.singletonList(abstractedFrom));

		return result;
	}

	private Relation relationFromEP(ExtendedProposition extendedProposition) {
		Relation result = new Relation();

		result.setLhsExtendedProposition(extendedProposition);
		result.setRhsExtendedProposition(extendedProposition);

		return result;
	}

	private ExtendedProposition epFromAbstraction(Proposition
														  abstraction,
												  Frequency element) {
		ExtendedProposition result = new ExtendedProposition();

		result.setProposition(abstraction);
		result.setValue(element.getDataElement().getWithValue());

		return result;
	}

	private final class IntermediateAbstractionGenerator implements
			PropositionEntityVisitor {

		final Frequency element;
		Proposition intermediateAbstraction;

		public IntermediateAbstractionGenerator(Frequency inElement) {
			element = inElement;
		}

		public Proposition getIntermediateAbstraction() {
			return intermediateAbstraction;
		}

		@Override
		public void visit(SystemProposition proposition) {
			throw new UnsupportedOperationException("SystemProposition not " +
					"supported");
		}

		@Override
		public void visit(Categorization categorization) {
			throw new UnsupportedOperationException("Categorization not " +
					"supported");
		}

		@Override
		public void visit(HighLevelAbstraction highLevelAbstraction) {
			throw new UnsupportedOperationException("HighLevelAbstraction not" +
					" supported");
		}

		@Override
		public void visit(SliceAbstraction sliceAbstraction) {
			throw new UnsupportedOperationException("SliceAbstraction not " +
					"supported");
		}

		@Override
		public void visit
				(LowLevelAbstraction original) {
			LowLevelAbstraction result = new LowLevelAbstraction();

			result.setUserId(original.getUserId());
			result.setInSystem(false);
			result.setKey(original.getKey() + INTERMEDIATE_SUFFIX);
			result.setDisplayName(original.getDisplayName() + INTERMEDIATE_SUFFIX);
			result.setAbstractedFrom(original.getAbstractedFrom());
			result.setUserConstraint(original.getUserConstraint());
			result.setComplementConstraint(original.getComplementConstraint());
			result.setCreatedFrom(LowLevelAbstraction.CreatedFrom.FREQUENCY);

			Date now = Calendar.getInstance().getTime();
			result.setCreated(now);
			result.setLastModified(now);

			result.setMinValues(element.getAtLeast());
			if (element.getIsWithin()) {
				if (element.getWithinAtLeast() != null && element
						.getWithinAtLeastUnits() != null) {
					result.setMinGapValues(element.getWithinAtLeast());
					result.setMinGapValuesUnits(timeUnitDao.retrieve(element.getWithinAtLeastUnits()));
				}
				if (element.getWithinAtMost() != null && element
						.getWithinAtMostUnits() != null) {
					result.setMaxGapValues(element.getWithinAtMost());
					result.setMaxGapValuesUnits(timeUnitDao.retrieve(element.getWithinAtMostUnits()));
				}
			}
			result.setAbstractedFrom(Collections.singletonList(propositionDao.getByUserAndKey(
					userId, element.getDataElement().getDataElementKey())));

			this.intermediateAbstraction = result;
		}

		@Override
		public void visit(
				CompoundLowLevelAbstraction original) {
			CompoundLowLevelAbstraction result = new CompoundLowLevelAbstraction();

			result.setUserId(original.getUserId());
			result.setInSystem(false);
			result.setKey(original.getKey() + INTERMEDIATE_SUFFIX);
			result.setDisplayName(original.getDisplayName() + INTERMEDIATE_SUFFIX);
			result.setAbstractedFrom(original.getAbstractedFrom());
			result.setUserValueDefinitionName(original.getUserValueDefinitionName());
			result.setComplementValueDefinitionName(original.getComplementValueDefinitionName());
			result.setCreatedFrom(CompoundLowLevelAbstraction.CreatedFrom.FREQUENCY);

			Date now = Calendar.getInstance().getTime();
			result.setCreated(now);
			result.setLastModified(now);

			result.setMinimumNumberOfValues(element.getAtLeast());

			if (element.getIsWithin()) {
				if (element.getWithinAtLeast() != null && element
						.getWithinAtLeastUnits() != null) {
					result.setMinimumGapBetweenValues(element.getWithinAtLeast());
					result.setMinimumGapBetweenValuesUnits(timeUnitDao
							.retrieve(element.getWithinAtLeastUnits()));
				}
				if (element.getWithinAtMost() != null && element
						.getWithinAtMostUnits() != null) {
					result.setMaximumGapBetweenValues(element.getWithinAtMost());
					result.setMaximumGapBetweenValuesUnits(timeUnitDao
							.retrieve(element.getWithinAtMostUnits()));
				}
			}

			intermediateAbstraction = result;
		}
	}

	@Override
	public Frequency translateFromProposition(HighLevelAbstraction
													  proposition) {
		Frequency result = new Frequency();

		PropositionTranslatorUtil.populateCommonDataElementFields(result, proposition);
//		result.setAtLeast(proposition.getMinValues());
//		result.setWithinAtLeast(proposition.getMinGapValues());
//		result.setWithinAtLeastUnits(proposition.getMinGapValuesUnits().getId());
//		result.setWithinAtMost(proposition.getMaxGapValues());
//		result.setWithinAtMostUnits(proposition.getMaxGapValuesUnits().getId());

		DataElementField dataElement = new DataElementField();
		dataElement.setDataElementKey(proposition.getAbstractedFrom().get(0).getKey());
		result.setDataElement(dataElement);

		return result;
	}

}