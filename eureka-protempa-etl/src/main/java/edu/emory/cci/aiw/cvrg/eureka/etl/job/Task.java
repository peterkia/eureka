/*
 * #%L
 * Eureka Protempa ETL
 * %%
 * Copyright (C) 2012 - 2013 Emory University
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
package edu.emory.cci.aiw.cvrg.eureka.etl.job;

import java.util.List;

import org.protempa.PropositionDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import edu.emory.cci.aiw.cvrg.eureka.common.entity.JobEntity;
import edu.emory.cci.aiw.cvrg.eureka.common.entity.JobEventType;
import edu.emory.cci.aiw.cvrg.eureka.etl.dao.JobDao;
import org.protempa.backend.dsb.filter.Filter;

public final class Task implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(Task.class);
	private final JobDao jobDao;
	private ETL etl;
	private Long jobId;
	private List<PropositionDefinition> propositionDefinitions;
	private List<String> propIdsToShow;
	private Filter filter;

	@Inject
	Task(JobDao inJobDao, ETL inEtl) {
		this.jobDao = inJobDao;
		this.etl = inEtl;
	}

	Long getJobId() {
		return jobId;
	}

	void setJobId(Long inJobId) {
		jobId = inJobId;
	}

	public Filter getFilter() {
		return filter;
	}

	public void setFilter(Filter filter) {
		this.filter = filter;
	}

	List<String> getPropositionIdsToShow() {
		return propIdsToShow;
	}

	void setPropositionIdsToShow(List<String> propIdsToShow) {
		this.propIdsToShow = propIdsToShow;
	}

	List<PropositionDefinition> getPropositionDefinitions() {
		return propositionDefinitions;
	}

	void setPropositionDefinitions(List<PropositionDefinition> inPropositionDefinitions) {
		propositionDefinitions = inPropositionDefinitions;
	}

	@Override
	public void run() {
		JobEntity myJob = null;
		try {
			myJob = this.jobDao.retrieve(this.jobId);
			LOGGER.info("{} just got a job from user {}, id={}",
					myJob.getEtlUser(), new Object[]{
				Thread.currentThread().getName(), myJob.toString()});
			myJob.newEvent(JobEventType.STARTED, "Processing started", null);
			this.jobDao.update(myJob);

			PropositionDefinition[] propDefArray =
					new PropositionDefinition[this.getPropositionDefinitions()
					.size()];
			this.propositionDefinitions.toArray(propDefArray);

			String[] propIdsToShowArray =
					this.propIdsToShow.toArray(
					new String[this.propIdsToShow.size()]);

			this.etl.run(myJob, propDefArray, propIdsToShowArray, this.filter);
			this.etl.close();
			this.etl = null;
			myJob.newEvent(JobEventType.COMPLETED, "Processing completed without error", null);
			this.jobDao.update(myJob);
			LOGGER.info("{} completed job {} for user {} without errors.",
					Thread.currentThread().getName(),
					new Object[]{myJob.getId(), myJob.getEtlUser()});
			myJob = null;
		} catch (EtlException e) {
			handleError(myJob, e);
		} catch (RuntimeException e) {
			handleError(myJob, e);
		} catch (Error e) {
			handleError(myJob, e);
		} finally {
			if (myJob != null) {
				try {
					myJob.newEvent(JobEventType.FAILED, "Processing failed", null);
					LOGGER.error("{} finished job {} for user {} with errors.",
							Thread.currentThread().getName(),
							new Object[]{myJob.getId(), myJob.getEtlUser()});
					this.jobDao.update(myJob);
				} catch (Throwable ignore) {
				}
			}
			if (this.etl != null) {
				try {
					this.etl.close();
				} catch (Throwable ignore) {
				}
			}
		}


	}

	private void handleError(JobEntity job, Throwable e) {
		if (job != null) {
			LOGGER.error("Job " + job.getId() + " for user "
					+ job.getEtlUser().getUsername() + " failed: " + e.getMessage(), e);
			StackTraceElement[] ste = e.getStackTrace();
			String[] st = new String[ste.length];
			for (int i = 0; i < ste.length; i++) {
				st[i] = ste[i].toString();
			}
			String msg = e.getMessage();
			if (msg == null) {
				msg = e.getClass().getName();
			}
			job.newEvent(JobEventType.ERROR, msg, st);
			this.jobDao.update(job);
		} else {
			LOGGER.error("Could not create job: " + e.getMessage(), e);
		}

	}
}
