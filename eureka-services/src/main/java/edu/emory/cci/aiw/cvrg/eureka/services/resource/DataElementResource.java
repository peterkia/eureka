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
package edu.emory.cci.aiw.cvrg.eureka.services.resource;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.inject.Inject;

import edu.emory.cci.aiw.cvrg.eureka.common.comm.DataElement;
import edu.emory.cci.aiw.cvrg.eureka.common.entity.CategoryEntity;
import edu.emory.cci.aiw.cvrg.eureka.common.entity.DataElementEntity;
import edu.emory.cci.aiw.cvrg.eureka.common.entity.ExtendedDataElement;
import edu.emory.cci.aiw.cvrg.eureka.common.entity.FrequencyEntity;
import edu.emory.cci.aiw.cvrg.eureka.common.entity.PropositionChildrenVisitor;
import edu.emory.cci.aiw.cvrg.eureka.common.entity.Relation;
import edu.emory.cci.aiw.cvrg.eureka.common.entity.SequenceEntity;
import edu.emory.cci.aiw.cvrg.eureka.common.exception.DataElementHandlingException;
import edu.emory.cci.aiw.cvrg.eureka.common.exception.HttpStatusException;
import edu.emory.cci.aiw.cvrg.eureka.services.dao.PropositionDao;
import edu.emory.cci.aiw.cvrg.eureka.services.translation.DataElementTranslatorVisitor;
import edu.emory.cci.aiw.cvrg.eureka.services.translation.PropositionTranslatorVisitor;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang.StringUtils;

/**
 * PropositionCh
 *
 * @author hrathod
 */
@Path("/dataelement")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DataElementResource {

	private static ResourceBundle messages =
			ResourceBundle.getBundle("Messages");
	private final PropositionDao propositionDao;
	private final SystemElementResource systemElementResource;
	private final PropositionTranslatorVisitor propositionTranslatorVisitor;
	private final DataElementTranslatorVisitor dataElementTranslatorVisitor;

	@Inject
	public DataElementResource(PropositionDao inDao, SystemElementResource inResource, PropositionTranslatorVisitor inPropositionTranslatorVisitor,
			DataElementTranslatorVisitor inDataElementTranslatorVisitor) {
		this.propositionDao = inDao;
		this.systemElementResource = inResource;
		this.propositionTranslatorVisitor = inPropositionTranslatorVisitor;
		this.dataElementTranslatorVisitor = inDataElementTranslatorVisitor;
	}

	@GET
	@Path("/{userId}")
	public List<DataElement> getAll(@PathParam("userId") Long inUserId) {

		List<DataElement> result = new ArrayList<DataElement>();
		List<DataElementEntity> propositions = this.propositionDao.getByUserId(inUserId);

		for (DataElementEntity proposition : propositions) {
			this.propositionDao.refresh(proposition);
			if (proposition.isInSystem()) {
				result.add(
						this.systemElementResource.get(
						inUserId, proposition.getKey()));
			} else {
				proposition.accept(this.propositionTranslatorVisitor);
				result.add(propositionTranslatorVisitor.getDataElement());
			}
		}

		return result;
	}

	@GET
	@Path("/{userId}/{key}")
	public DataElement get(@PathParam("userId") Long inUserId,
			@PathParam("key") String inKey) {
		DataElement result;
		DataElementEntity proposition = this.propositionDao.getByUserAndKey(inUserId, inKey);
		if (proposition == null || proposition.isInSystem()) {
			result = this.systemElementResource.get(inUserId, inKey);
		} else {
			proposition.accept(this.propositionTranslatorVisitor);
			result = this.propositionTranslatorVisitor.getDataElement();
		}
		return result;
	}

	@POST
	public void create(DataElement inElement) {
		if (inElement.getId() != null) {
			throw new HttpStatusException(
					Response.Status.PRECONDITION_FAILED, "Data element to "
					+ "be created should not have an identifier.");
		}

		if (inElement.getUserId() == null) {
			throw new HttpStatusException(
					Response.Status.PRECONDITION_FAILED, "Data element to "
					+ "be created should have a user identifier.");
		}

		try {
			inElement.accept(this.dataElementTranslatorVisitor);
		} catch (DataElementHandlingException ex) {
			throw new HttpStatusException(ex.getStatus(), ex);
		}
		DataElementEntity proposition = this.dataElementTranslatorVisitor
				.getProposition();
		
		if (this.propositionDao.getByUserAndKey(
				inElement.getUserId(), proposition.getKey()) != null) {
			String msg = this.messages.getString(
					"dataElementResource.create.error.duplicate");
			throw new HttpStatusException(Status.CONFLICT, msg);
		}
		
		Date now = new Date();
		proposition.setCreated(now);
		proposition.setLastModified(now);

		this.propositionDao.create(proposition);
	}

	@PUT
	public void update(DataElement inElement) {
		if (inElement.getId() == null) {
			throw new HttpStatusException(Response.Status.PRECONDITION_FAILED,
					"Data element to be updated must "
					+ "have a unique identifier.");
		}

		if (inElement.getUserId() == null) {
			throw new HttpStatusException(Response.Status.PRECONDITION_FAILED,
					"Data element to be updated must "
					+ "have a user identifier");
		}
		try {
			inElement.accept(this.dataElementTranslatorVisitor);
		} catch (DataElementHandlingException ex) {
			throw new HttpStatusException(
					Response.Status.INTERNAL_SERVER_ERROR, ex);
		}
		DataElementEntity proposition = this.dataElementTranslatorVisitor
				.getProposition();
		DataElementEntity oldProposition = this.propositionDao.retrieve(inElement
				.getId());

		if (oldProposition == null) {
			throw new HttpStatusException(Response.Status.NOT_FOUND);
		} else if (!oldProposition.getUserId().equals(inElement.getUserId())) {
			throw new HttpStatusException(Response.Status.NOT_FOUND);
		} else if (this.propositionDao.getByUserAndKey(
				inElement.getUserId(), proposition.getKey()) != null) {
			String msg = this.messages.getString(
					"dataElementResource.create.error.duplicate");
			throw new HttpStatusException(Status.CONFLICT, msg);
		}

		Date now = new Date();
		proposition.setLastModified(now);
		proposition.setCreated(oldProposition.getCreated());
		proposition.setId(oldProposition.getId());
		this.propositionDao.update(proposition);
	}

	@DELETE
	@Path("/{userId}/{key}")
	public void delete(@PathParam("userId") Long inUserId,
			@PathParam("key") String inKey) {
		System.err.print("delete: " + inKey);
		DataElementEntity proposition =
				this.propositionDao.getByUserAndKey(inUserId, inKey);
		if (proposition == null) {
			throw new HttpStatusException(Response.Status.NOT_FOUND);
		}
		List<DataElementEntity> others =
				this.propositionDao.getByUserId(inUserId);
		List<String> dataElementsUsedIn = new ArrayList<String>();
		for (DataElementEntity other : others) {
			if (!other.getId().equals(proposition.getId())) {
				PropositionChildrenVisitor visitor =
						new PropositionChildrenVisitor();
				other.accept(visitor);
				for (DataElementEntity child : visitor.getChildren()) {
					if (child.getId().equals(proposition.getId())) {
						dataElementsUsedIn.add(other.getDisplayName());
					}
				}
			}
		}
		if (!dataElementsUsedIn.isEmpty()) {
			String dataElementList;
			int size = dataElementsUsedIn.size();
			if (size > 1) {
				List<String> subList =
						dataElementsUsedIn.subList(0,
						dataElementsUsedIn.size() - 1);
				dataElementList = StringUtils.join(subList, ", ")
						+ " and "
						+ dataElementsUsedIn.get(size - 1);
			} else {
				dataElementList = dataElementsUsedIn.get(0);
			}
			MessageFormat usedByOtherDataElements =
					new MessageFormat(messages.getString(
					"dataElementResource.delete.error.usedByOtherDataElements"));
			String msg = usedByOtherDataElements.format(
					new Object[]{
						proposition.getDisplayName(),
						dataElementsUsedIn.size(),
						dataElementList
					});
			throw new HttpStatusException(
					Response.Status.PRECONDITION_FAILED, msg);
		}
		

//		if (proposition instanceof FrequencyEntity) {
//			ExtendedDataElement extendedProposition = ((FrequencyEntity) proposition).getExtendedProposition();
//			extendedProposition.getDataElementEntity().getExtendedDataElements().remove(extendedProposition);
//		} else if (proposition instanceof SequenceEntity) {
//			ExtendedDataElement extendedProposition = ((SequenceEntity) proposition).getPrimaryExtendedDataElement();
//			extendedProposition.getDataElementEntity().getExtendedDataElements().remove(extendedProposition);
//			for (Relation relation : ((SequenceEntity) proposition).getRelations()) {
//				ExtendedDataElement lhs = relation.getLhsExtendedDataElement();
//				lhs.getDataElementEntity().getExtendedDataElements().remove(lhs);
//				ExtendedDataElement rhs = relation.getRhsExtendedDataElement();
//				rhs.getDataElementEntity().getExtendedDataElements().remove(rhs);
//			}
//		}

		this.propositionDao.remove(proposition);
	}
	/*
	 @Path("/systemelement")
	 public SystemElementResource getSystemElementResource() {
	 return this.systemElementResource;
	 }
	 */
}
