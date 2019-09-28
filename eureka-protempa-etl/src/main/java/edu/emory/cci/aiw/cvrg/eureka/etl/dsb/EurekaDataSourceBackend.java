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
package edu.emory.cci.aiw.cvrg.eureka.etl.dsb;

import edu.emory.cci.aiw.cvrg.eureka.etl.spreadsheet.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.arp.javautil.io.FileUtil;
import org.arp.javautil.io.IOUtil;
import org.arp.javautil.sql.InvalidConnectionSpecArguments;
import org.arp.javautil.sql.SQLExecutor;
import org.protempa.*;
import org.protempa.backend.BackendInitializationException;
import org.protempa.backend.BackendInstanceSpec;
import org.protempa.backend.DataSourceBackendFailedDataValidationException;
import org.protempa.backend.DataSourceBackendInitializationException;
import org.protempa.backend.annotations.BackendInfo;
import org.protempa.backend.annotations.BackendProperty;
import org.protempa.backend.dsb.DataValidationEvent;
import org.protempa.backend.dsb.filter.Filter;
import org.protempa.backend.dsb.relationaldb.ColumnSpec;
import org.protempa.backend.dsb.relationaldb.EntitySpec;
import org.protempa.backend.dsb.relationaldb.JDBCDateTimeTimestampDateValueFormat;
import org.protempa.backend.dsb.relationaldb.JDBCDateTimeTimestampPositionParser;
import org.protempa.backend.dsb.relationaldb.JDBCPositionFormat;
import org.protempa.backend.dsb.relationaldb.JoinSpec;
import org.protempa.backend.dsb.relationaldb.Operator;
import org.protempa.backend.dsb.relationaldb.PropertySpec;
import org.protempa.backend.dsb.relationaldb.ReferenceSpec;
import org.protempa.backend.dsb.relationaldb.RelationalDbDataSourceBackend;
import org.protempa.backend.dsb.relationaldb.StagingSpec;
import org.protempa.backend.dsb.relationaldb.mappings.Mappings;
import org.protempa.backend.dsb.relationaldb.mappings.ResourceMappingsFactory;
import org.protempa.dest.QueryResultsHandler;
import org.protempa.proposition.Proposition;
import org.protempa.proposition.value.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data source backend for Eureka!.
 *
 * @author Andrew Post
 */
@BackendInfo(displayName = "Eureka Spreadsheet Data Source Backend")
public final class EurekaDataSourceBackend extends RelationalDbDataSourceBackend implements EurekaFileDataSourceBackend {

	private static Logger LOGGER = LoggerFactory.getLogger(EurekaDataSourceBackend.class);
	private static JDBCPositionFormat dtPositionParser
			= new JDBCDateTimeTimestampPositionParser();
	private static final String DEFAULT_ROOT_FULL_NAME = "Eureka";
	private XlsxDataProvider[] dataProviders = null;
	private boolean dataPopulated;
	private String sampleUrl;
	private String databaseName;
	private String labsRootFullName;
	private String vitalsRootFullName;
	private String diagnosisCodesRootFullName;
	private String medicationOrdersRootFullName;
	private String icd9ProcedureCodesRootFullName;
	private String cptProcedureCodesRootFullName;
	private final FileDataSourceBackendSupport fileDataSourceBackendSupport;

	public EurekaDataSourceBackend() {
		setSchemaName("EUREKA");
		setDefaultKeyIdTable("PATIENT");
		setDefaultKeyIdColumn("PATIENT_KEY");
		setDefaultKeyIdJoinKey("PATIENT_KEY");
		this.labsRootFullName = DEFAULT_ROOT_FULL_NAME;
		this.vitalsRootFullName = DEFAULT_ROOT_FULL_NAME;
		this.diagnosisCodesRootFullName = DEFAULT_ROOT_FULL_NAME;
		this.medicationOrdersRootFullName = DEFAULT_ROOT_FULL_NAME;
		this.icd9ProcedureCodesRootFullName = DEFAULT_ROOT_FULL_NAME;
		this.cptProcedureCodesRootFullName = DEFAULT_ROOT_FULL_NAME;
		setMappingsFactory(new ResourceMappingsFactory("/mappings/", getClass()));
		this.fileDataSourceBackendSupport = new FileDataSourceBackendSupport(nameForErrors());
		this.fileDataSourceBackendSupport.setDataFileDirectoryName("filename");
	}

	@Override
	public void initialize(BackendInstanceSpec config) throws BackendInitializationException {
		super.initialize(config);
		try {
			Class.forName("org.h2.Driver");
		} catch (ClassNotFoundException ex) {
			throw new DataSourceBackendInitializationException("The H2 database driver is not registered", ex);
		}
		String databaseName = getDatabaseId();
		if (databaseName == null) {
			throw new DataSourceBackendInitializationException("No database name specified for data source backend '" + nameForErrors() + "'");
		}
		File schemaFile;
		String schemaFilePath;
		try {
			schemaFile = IOUtil.resourceToFile("/eureka-dsb-schema.sql", "eureka-dsb-schema", ".sql");
			/*** windows file system  ****/
			schemaFilePath = ""+FilenameUtils.separatorsToUnix(schemaFile.getPath());
		} catch (IOException ex) {
			throw new DataSourceBackendInitializationException("Unable to create data schema (data source backend '" + nameForErrors() + "')");
		}
		super.setDatabaseId("jdbc:h2:mem:" + databaseName + ";INIT=RUNSCRIPT FROM '" + schemaFilePath + "';DB_CLOSE_DELAY=-1;LOG=0;LOCK_MODE=0;UNDO_LOG=0");
		this.fileDataSourceBackendSupport.setConfigurationsId(getConfigurationsId());
		File[] dataFiles;
		try {
			dataFiles = this.fileDataSourceBackendSupport.getUploadedFiles();
		} catch (IOException ex) {
			throw new DataSourceBackendInitializationException("Error initializing data source backend " + nameForErrors(), ex);
		}
		if (dataFiles != null) {
			this.dataProviders = new XlsxDataProvider[dataFiles.length];
			for (int i = 0; i < dataFiles.length; i++) {
				LOGGER.info("Reading spreadsheet {}", dataFiles[i].getAbsolutePath());
				if (!dataFiles[i].exists()) {
					throw new DataSourceBackendInitializationException("Error initializing data source backend " + this.nameForErrors(), new FileNotFoundException(dataFiles[i].getAbsolutePath()));
				}
				try {
					this.dataProviders[i] = new XlsxDataProvider(dataFiles[i], null);
				} catch (DataProviderException ex) {
					dataFiles[i].renameTo(FileUtil.replaceExtension(dataFiles[i], ".failed"));
					for (int j = 0; j < i; j++) {
						try {
							this.dataProviders[j].close();
						} catch (IOException ignore) {
						}
					}
					throw new DataSourceBackendInitializationException("Error initializing data source backend " + this.nameForErrors(), ex);
				}
			}
		}
	}

	@Override
	public DataValidationEvent[] validateData(KnowledgeSource knowledgeSource) throws DataSourceBackendFailedDataValidationException, KnowledgeSourceReadException {
		List<DataValidationEvent> events = new ArrayList<>();
		boolean failedValidation = false;
		if (this.dataProviders != null) {
			for (XlsxDataProvider dataProvider : this.dataProviders) {
				DataValidator dataValidator = new DataValidator(dataProvider.getDataFile());
				try {
					dataValidator.setPatients(dataProvider.getPatients())
							.setEncounters(dataProvider.getEncounters())
							.setProviders(dataProvider.getProviders())
							.setCpts(dataProvider.getCptCodes())
							.setIcd9Procedures(dataProvider.getIcd9Procedures())
							.setIcd9Diagnoses(dataProvider.getIcd9Diagnoses())
							.setMedications(dataProvider.getMedications())
							.setLabs(dataProvider.getLabs())
							.setVitals(dataProvider.getVitals()).validate();
				} catch (DataProviderException e) {
					throw new DataSourceBackendFailedDataValidationException(e, null);
				}
				events.addAll(dataValidator.getValidationEvents());

				if (dataValidator.isFailed()) {
					failedValidation = true;
				}
			}
		}
		DataValidationEvent[] validationEvents = events.toArray(new DataValidationEvent[events.size()]);
		if (failedValidation) {
			throw new DataSourceBackendFailedDataValidationException("Invalid spreadsheet " + this.fileDataSourceBackendSupport.getDataFileDirectoryName() + " in data source backend " + nameForErrors(), validationEvents);
		} else {
			return validationEvents;
		}
	}

	private void populateDatabase() throws DataSourceReadException {
		Connection dataInserterConnection = null;
		Throwable exceptionThrown = null;
		try {
			dataInserterConnection
					= getConnectionSpecInstance().getOrCreate();
			if (this.dataProviders != null) {
				for (DataProvider dataProvider : this.dataProviders) {
					DataInserter dataInserter
							= new DataInserter(dataInserterConnection);
					dataInserter.insertPatients(dataProvider.getPatients());
					dataInserterConnection.commit();
					dataInserter.insertEncounters(dataProvider.getEncounters());
					dataInserterConnection.commit();
					dataInserter.insertProviders(dataProvider.getProviders());
					dataInserterConnection.commit();
					dataInserter.insertCptCodes(dataProvider.getCptCodes());
					dataInserterConnection.commit();
					dataInserter.insertIcd9Diagnoses(dataProvider
							.getIcd9Diagnoses());
					dataInserterConnection.commit();
					dataInserter.insertIcd9Procedures(dataProvider
							.getIcd9Procedures());
					dataInserterConnection.commit();
					dataInserter.insertLabs(dataProvider.getLabs());
					dataInserterConnection.commit();
					dataInserter.insertMedications(dataProvider.getMedications());
					dataInserterConnection.commit();
					dataInserter.insertVitals(dataProvider.getVitals());
					dataInserterConnection.commit();
				}
			}
			this.dataPopulated = true;
			dataInserterConnection.close();
		} catch (SQLException | DataProviderException | DataInserterException | InvalidConnectionSpecArguments sqle) {
			exceptionThrown = sqle;
			if (dataInserterConnection != null) {
				try {
					dataInserterConnection.rollback();
				} catch (SQLException ignore) {
					sqle.addSuppressed(ignore);
				}
			}
			throw new DataSourceReadException("Error reading spreadsheets in " + this.fileDataSourceBackendSupport.getDataFileDirectoryName() + " in data source backend " + nameForErrors(), sqle);
		} finally {
			if (dataInserterConnection != null) {
				try {
					dataInserterConnection.close();
				} catch (SQLException ignore) {
					if (exceptionThrown != null) {
						exceptionThrown.addSuppressed(ignore);
					}
				}
			}
		}
	}

	@Override
	public DataStreamingEventIterator<Proposition> readPropositions(Set<String> keyIds, Set<String> propIds, Filter filters, QueryResultsHandler queryResultsHandler) throws DataSourceReadException {
		if (!dataPopulated) {
			populateDatabase();
		}
		return super.readPropositions(keyIds, propIds, filters, queryResultsHandler);
	}

	@Override
	protected StagingSpec[] stagedSpecs(String keyIdSchema, String keyIdTable, String keyIdColumn, String keyIdJoinKey) throws IOException {
		return null;
	}

	@Override
	public String getKeyType() {
		return "Patient";
	}

	@Override
	public String getKeyTypeDisplayName() {
		return "patient";
	}

	@Override
	public String getDatabaseId() {
		return this.databaseName;
	}

	@Override
	@BackendProperty(propertyName = "databaseName")
	public void setDatabaseId(String databaseId) {
		this.databaseName = databaseId;
	}

	@BackendProperty(
			displayName = "Download Sample",
			description = "Use this sample spreadsheet to guide you in creating a spreadsheet containing your own data.",
			validator = UriBackendPropertyValidator.class)
	public void setSampleUrl(String sampleUrl) {
		this.sampleUrl = sampleUrl;
	}

	public String getSampleUrl() {
		return sampleUrl;
	}

	@BackendProperty(
			displayName = "Excel Spreadsheet",
			description = "An Excel spreadsheet as described in the provided sample (see Download Sample).",
			validator = ExcelSpreadsheetBackendPropertyValidator.class,
			required = true
	)
	@Override
	public void setFilename(String filename) {
		this.fileDataSourceBackendSupport.setFilename(filename);
	}

	@Override
	public String getFilename() {
		return this.fileDataSourceBackendSupport.getFilename();
	}

	@BackendProperty
	public void setLabsRootFullName(String labsRootFullName) {
		if (labsRootFullName == null) {
			this.labsRootFullName = DEFAULT_ROOT_FULL_NAME;
		} else {
			this.labsRootFullName = labsRootFullName;
		}
	}

	public String getLabsRootFullName() {
		return labsRootFullName;
	}

	public String getVitalsRootFullName() {
		return vitalsRootFullName;
	}

	@BackendProperty
	public void setVitalsRootFullName(String vitalsRootFullName) {
		if (vitalsRootFullName == null) {
			this.vitalsRootFullName = DEFAULT_ROOT_FULL_NAME;
		} else {
			this.vitalsRootFullName = vitalsRootFullName;
		}
	}

	public String getDiagnosisCodesRootFullName() {
		return diagnosisCodesRootFullName;
	}

	@BackendProperty
	public void setDiagnosisCodesRootFullName(String diagnosisCodesRootFullName) {
		if (diagnosisCodesRootFullName == null) {
			this.diagnosisCodesRootFullName = DEFAULT_ROOT_FULL_NAME;
		} else {
			this.diagnosisCodesRootFullName = diagnosisCodesRootFullName;
		}
	}

	public String getMedicationOrdersRootFullName() {
		return medicationOrdersRootFullName;
	}

	@BackendProperty
	public void setMedicationOrdersRootFullName(String medicationOrdersRootFullName) {
		if (medicationOrdersRootFullName == null) {
			this.medicationOrdersRootFullName = DEFAULT_ROOT_FULL_NAME;
		} else {
			this.medicationOrdersRootFullName = medicationOrdersRootFullName;
		}
	}

	public String getIcd9ProcedureCodesRootFullName() {
		return icd9ProcedureCodesRootFullName;
	}

	@BackendProperty
	public void setIcd9ProcedureCodesRootFullName(String icd9ProcedureCodesRootFullName) {
		if (icd9ProcedureCodesRootFullName == null) {
			this.icd9ProcedureCodesRootFullName = DEFAULT_ROOT_FULL_NAME;
		} else {
			this.icd9ProcedureCodesRootFullName = icd9ProcedureCodesRootFullName;
		}
	}

	public String getCptProcedureCodesRootFullName() {
		return cptProcedureCodesRootFullName;
	}

	@BackendProperty
	public void setCptProcedureCodesRootFullName(String cptProcedureCodesRootFullName) {
		if (cptProcedureCodesRootFullName == null) {
			this.cptProcedureCodesRootFullName = DEFAULT_ROOT_FULL_NAME;
		} else {
			this.cptProcedureCodesRootFullName = cptProcedureCodesRootFullName;
		}
	}

	@Override
	protected EntitySpec[] constantSpecs(String keyIdSchema, String keyIdTable, String keyIdColumn, String keyIdJoinKey) throws IOException {
		String schemaName = getSchemaName();
		EntitySpec[] constantSpecs = new EntitySpec[]{
			new EntitySpec("Patients", null, new String[]{"Patient"},
			false, new ColumnSpec(keyIdSchema,
			keyIdTable,
			keyIdColumn), new ColumnSpec[]{
				new ColumnSpec(keyIdSchema, keyIdTable,
				keyIdColumn)}, null, null,
			new PropertySpec[]{
				new PropertySpec("patientId", null,
				new ColumnSpec(keyIdSchema,
				keyIdTable,
				"PATIENT_KEY"),
				ValueType.NOMINALVALUE)},
			new ReferenceSpec[]{}, null, null,
			null, null, null, null, null, null),
			new EntitySpec("Patient Details", null, new String[]{"PatientDetails"},
			true, new ColumnSpec(keyIdSchema, keyIdTable,
			keyIdColumn), new ColumnSpec[]{
				new ColumnSpec(schemaName, keyIdTable,
				"PATIENT_KEY")}, null, null, new PropertySpec[]{
				new PropertySpec("dateOfBirth", null,
				new ColumnSpec(keyIdSchema,
				keyIdTable, "DOB"),
				ValueType.DATEVALUE,
				new JDBCDateTimeTimestampDateValueFormat()),
				new PropertySpec("patientId", null,
				new ColumnSpec(keyIdSchema,
				keyIdTable, "PATIENT_KEY"),
				ValueType.NOMINALVALUE),
				new PropertySpec("firstName", null,
				new ColumnSpec(schemaName, "PATIENT",
				"FIRST_NAME"), ValueType.NOMINALVALUE),
				new PropertySpec("lastName", null,
				new ColumnSpec(schemaName, "PATIENT",
				"LAST_NAME"), ValueType.NOMINALVALUE),
				new PropertySpec("gender", null,
				new ColumnSpec(schemaName, "PATIENT", "GENDER",
				Operator.EQUAL_TO,
				getMappingsFactory()
				.getInstance(
				"gender_08172011.txt"),
				true), ValueType.NOMINALVALUE),
				new PropertySpec(
				"maritalStatus",
				null,
				new ColumnSpec(
				schemaName,
				"PATIENT",
				"MARITAL_STATUS",
				Operator.EQUAL_TO,
				getMappingsFactory()
				.getInstance("marital_status_08172011.txt"),
				true), ValueType.NOMINALVALUE),
				new PropertySpec(
				"language",
				null,
				new ColumnSpec(
				schemaName,
				"PATIENT",
				"LANGUAGE",
				Operator.EQUAL_TO,
				getMappingsFactory()
				.getInstance("language_08152012.txt"),
				true), ValueType.NOMINALVALUE),
				new PropertySpec("race", null,
				new ColumnSpec(schemaName, "PATIENT", "RACE",
				Operator.EQUAL_TO,
				getMappingsFactory()
				.getInstance(
				"race_08172011.txt"),
				true), ValueType.NOMINALVALUE),
				new PropertySpec("ethnicity", null,
				new ColumnSpec(schemaName, "PATIENT", "RACE",
				Operator.EQUAL_TO,
				getMappingsFactory()
				.getInstance(
				"ethnicity_08172011.txt"),
				true), ValueType.NOMINALVALUE)},
			new ReferenceSpec[]{
				new ReferenceSpec("encounters", "Encounters",
				new ColumnSpec[]{
					new ColumnSpec(schemaName,
					"PATIENT", new JoinSpec(
					"PATIENT_KEY",
					"PATIENT_KEY",
					new ColumnSpec(
					schemaName,
					"ENCOUNTER",
					"ENCOUNTER_KEY")))},
				ReferenceSpec.Type.MANY),
				new ReferenceSpec("patient", "Patients",
				new ColumnSpec[]{
					new ColumnSpec(schemaName,
					"PATIENT",
					"PATIENT_KEY")},
				ReferenceSpec.Type.ONE)}, null, null,
			null, null, null, null, null, null),
			new EntitySpec("Providers", null,
			new String[]{"Provider"}, false,
			new ColumnSpec(keyIdSchema, keyIdTable,
			keyIdColumn,
			new JoinSpec("PATIENT_KEY", "PATIENT_KEY",
			new ColumnSpec(schemaName, "ENCOUNTER",
			new JoinSpec("PROVIDER_KEY",
			"PROVIDER_KEY",
			new ColumnSpec(
			schemaName,
			"PROVIDER"))))),
			new ColumnSpec[]{
				new ColumnSpec(schemaName, "PROVIDER",
				"PROVIDER_KEY")}, null, null,
			new PropertySpec[]{
				new PropertySpec("firstName", null,
				new ColumnSpec(schemaName, "PROVIDER",
				"FIRST_NAME"),
				ValueType.NOMINALVALUE),
				new PropertySpec("lastName", null,
				new ColumnSpec(schemaName, "PROVIDER",
				"LAST_NAME"),
				ValueType.NOMINALVALUE)}, null, null,
			null, null, null, null, null, null, null),};
		return constantSpecs;
	}

	@Override
	protected EntitySpec[] eventSpecs(String keyIdSchema, String keyIdTable, String keyIdColumn, String keyIdJoinKey) throws IOException {
		String schemaName = getSchemaName();
		Mappings icd9DxMappings = getMappingsFactory().getInstance("icd9_diagnosis_08172011.txt");
		Mappings icd9PxMappings = getMappingsFactory().getInstance("icd9_procedure_08172011.txt");
		Mappings cptMappings = getMappingsFactory().getInstance("cpt_procedure_08172011.txt");
		Mappings medsMappings = getMappingsFactory().getInstance("meds_08182011.txt");
		EntitySpec[] eventSpecs = new EntitySpec[]{
			new EntitySpec("Encounters", null, new String[]{"Encounter"},
			true, new ColumnSpec(keyIdSchema, keyIdTable,
			keyIdColumn,
			new JoinSpec("PATIENT_KEY", "PATIENT_KEY",
			new ColumnSpec(schemaName, "ENCOUNTER"))),
			new ColumnSpec[]{
				new ColumnSpec(schemaName, "ENCOUNTER",
				"ENCOUNTER_KEY")},
			new ColumnSpec(schemaName, "ENCOUNTER", "TS_START"),
			new ColumnSpec(schemaName, "ENCOUNTER", "TS_END"),
			new PropertySpec[]{
				new PropertySpec("encounterId", null,
				new ColumnSpec(schemaName, "ENCOUNTER",
				"ENCOUNTER_KEY"),
				ValueType.NOMINALVALUE),
				new PropertySpec("type", null,
				new ColumnSpec(schemaName, "ENCOUNTER",
				"ENCOUNTER_TYPE",
				Operator.EQUAL_TO,
				getMappingsFactory()
				.getInstance(
				"type_encounter_08172011.txt"),
				true), ValueType.NOMINALVALUE),
				/*new PropertySpec("healthcareEntity", null, new ColumnSpec(schemaName, "ENCOUNTER", "UNIVCODE", ColumnSpec.Operator.EQUAL_TO, this.mapper.read("entity_healthcare_07182011.txt"), true), ValueType.NOMINALVALUE), */
				new PropertySpec("dischargeDisposition", null,
				new ColumnSpec(schemaName, "ENCOUNTER",
				"DISCHARGE_DISP",
				Operator.EQUAL_TO,
				getMappingsFactory()
				.getInstance(
				"disposition_discharge_08172011.txt"),
				true), ValueType.NOMINALVALUE), /*new PropertySpec("aprdrgRiskMortalityValue", null, new ColumnSpec(schemaName, "ENCOUNTER", "APRRISKOFMORTALITY"), ValueType.NUMERICALVALUE), new PropertySpec("aprdrgSeverityValue", null, new ColumnSpec(schemaName, "ENCOUNTER", "APRSEVERITYOFILLNESS"), ValueType.NOMINALVALUE), new PropertySpec("insuranceType", null, new ColumnSpec(schemaName, "ENCOUNTER", "HOSPITALPRIMARYPAYER", ColumnSpec.Operator.EQUAL_TO, this.mapper.read("insurance_types_07182011.txt")), ValueType.NOMINALVALUE)*/},
			new ReferenceSpec[]{
				new ReferenceSpec(this.labsRootFullName, "Labs",
				new ColumnSpec[]{
					new ColumnSpec(schemaName,
					"ENCOUNTER",
					new JoinSpec(
					"ENCOUNTER_KEY",
					"ENCOUNTER_KEY",
					new ColumnSpec(
					schemaName,
					"LABS_EVENT",
					"EVENT_KEY")))},
				ReferenceSpec.Type.MANY),
				new ReferenceSpec(this.vitalsRootFullName, "Vitals",
				new ColumnSpec[]{
					new ColumnSpec(schemaName,
					"ENCOUNTER",
					new JoinSpec(
					"ENCOUNTER_KEY",
					"ENCOUNTER_KEY",
					new ColumnSpec(
					schemaName,
					"VITALS_EVENT",
					"EVENT_KEY")))},
				ReferenceSpec.Type.MANY),
				new ReferenceSpec(this.diagnosisCodesRootFullName,
				"Diagnosis Codes", new ColumnSpec[]{
					new ColumnSpec(schemaName, "ENCOUNTER",
					new JoinSpec("ENCOUNTER_KEY",
					"ENCOUNTER_KEY",
					new ColumnSpec(
					schemaName,
					"ICD9D_EVENT",
					"EVENT_KEY")))},
				ReferenceSpec.Type.MANY),
				new ReferenceSpec(this.medicationOrdersRootFullName,
				"Medication Orders", new ColumnSpec[]{
					new ColumnSpec(schemaName, "ENCOUNTER",
					new JoinSpec("ENCOUNTER_KEY",
					"ENCOUNTER_KEY",
					new ColumnSpec(
					schemaName,
					"MEDS_EVENT",
					"EVENT_KEY")))},
				ReferenceSpec.Type.MANY),
				new ReferenceSpec(this.icd9ProcedureCodesRootFullName,
				"ICD9 Procedure Codes",
				new ColumnSpec[]{
					new ColumnSpec(schemaName,
					"ENCOUNTER",
					new JoinSpec(
					"ENCOUNTER_KEY",
					"ENCOUNTER_KEY",
					new ColumnSpec(
					schemaName,
					"ICD9P_EVENT",
					"EVENT_KEY")))},
				ReferenceSpec.Type.MANY),
				new ReferenceSpec(this.cptProcedureCodesRootFullName,
				"CPT Procedure Codes", new ColumnSpec[]{
					new ColumnSpec(schemaName, "ENCOUNTER",
					new JoinSpec("ENCOUNTER_KEY",
					"ENCOUNTER_KEY",
					new ColumnSpec(
					schemaName,
					"CPT_EVENT",
					"EVENT_KEY")))},
				ReferenceSpec.Type.MANY),
				/*new ReferenceSpec("msdrg", "MSDRG Codes", new ColumnSpec[]{new ColumnSpec(schemaName, "ENCOUNTER", "RECORD_ID")}, ReferenceSpec.Type.ONE), new ReferenceSpec("aprdrg", "APR DRG Codes", new ColumnSpec[]{new ColumnSpec(schemaName, "ENCOUNTER", "RECORD_ID")}, ReferenceSpec.Type.ONE), */
				new ReferenceSpec("provider", "Providers",
				new ColumnSpec[]{
					new ColumnSpec(schemaName,
					"ENCOUNTER",
					"PROVIDER_KEY")},
				ReferenceSpec.Type.ONE),
				/*new ReferenceSpec("attendingPhysician", "AttendingPhysicians", new ColumnSpec[]{new ColumnSpec(schemaName, "ENCOUNTER", "DISCHARGEPHYSICIAN")}, ReferenceSpec.Type.ONE), */
				new ReferenceSpec("patientDetails",
				"Patient Details", new ColumnSpec[]{
					new ColumnSpec(schemaName, "ENCOUNTER",
					"PATIENT_KEY")},
				ReferenceSpec.Type.ONE), /*new ReferenceSpec("chargeAmount", "Hospital Charge Amount", new ColumnSpec[]{new ColumnSpec(schemaName, "ENCOUNTER", "RECORD_ID")}, ReferenceSpec.Type.ONE)*/},
			null, null, null, null, null,
			AbsoluteTimeGranularity.DAY, dtPositionParser, null),
			new EntitySpec("Diagnosis Codes", null, icd9DxMappings.readTargets(),
			true, new ColumnSpec(keyIdSchema, keyIdTable,
			keyIdColumn,
			new JoinSpec("PATIENT_KEY", "PATIENT_KEY",
			new ColumnSpec(schemaName, "ENCOUNTER",
			new JoinSpec("ENCOUNTER_KEY",
			"ENCOUNTER_KEY",
			new ColumnSpec(schemaName,
			"ICD9D_EVENT"))))),
			new ColumnSpec[]{
				new ColumnSpec(schemaName, "ICD9D_EVENT",
				"EVENT_KEY")},
			new ColumnSpec(schemaName, "ICD9D_EVENT", "TS_OBX"),
			null, new PropertySpec[]{
				new PropertySpec("code", null,
				new ColumnSpec(schemaName, "ICD9D_EVENT",
				"ENTITY_ID"),
				ValueType.NOMINALVALUE),
				new PropertySpec("DXPRIORITY",
				null,
				new ColumnSpec(schemaName, "ICD9D_EVENT",
				"RANK",
				Operator.EQUAL_TO,
				getMappingsFactory().getInstance("icd9_diagnosis_position_07182011.txt")),
				ValueType.NOMINALVALUE)},
			new ReferenceSpec[]{
				new ReferenceSpec("encounter", "Encounters", new ColumnSpec[]{new ColumnSpec(schemaName, "ICD9D_EVENT", "ENCOUNTER_KEY")}, ReferenceSpec.Type.ONE)
			}, null,
			new ColumnSpec(schemaName, "ICD9D_EVENT", "ENTITY_ID",
			Operator.EQUAL_TO, icd9DxMappings, true),
			null, null, null, AbsoluteTimeGranularity.MINUTE,
			dtPositionParser, null),
			new EntitySpec("ICD9 Procedure Codes", null, icd9PxMappings.readTargets(),
			true, new ColumnSpec(keyIdSchema, keyIdTable,
			keyIdColumn,
			new JoinSpec("PATIENT_KEY", "PATIENT_KEY",
			new ColumnSpec(schemaName, "ENCOUNTER",
			new JoinSpec("ENCOUNTER_KEY",
			"ENCOUNTER_KEY",
			new ColumnSpec(schemaName,
			"ICD9P_EVENT"))))),
			new ColumnSpec[]{
				new ColumnSpec(schemaName, "ICD9P_EVENT",
				"EVENT_KEY")},
			new ColumnSpec(schemaName, "ICD9P_EVENT", "TS_OBX"),
			null, new PropertySpec[]{
				new PropertySpec("code", null,
				new ColumnSpec(schemaName, "ICD9P_EVENT",
				"ENTITY_ID"),
				ValueType.NOMINALVALUE)}, 
			new ReferenceSpec[]{
				new ReferenceSpec("encounter", "Encounters", new ColumnSpec[]{new ColumnSpec(schemaName, "ICD9P_EVENT", "ENCOUNTER_KEY")}, ReferenceSpec.Type.ONE)
			}, null,
			new ColumnSpec(schemaName, "ICD9P_EVENT", "ENTITY_ID",
			Operator.EQUAL_TO, icd9PxMappings, true),
			null, null, null, AbsoluteTimeGranularity.MINUTE,
			dtPositionParser, null),
			new EntitySpec("CPT Procedure Codes", null, cptMappings.readTargets(), true,
			new ColumnSpec(keyIdSchema, keyIdTable,
			keyIdColumn,
			new JoinSpec("PATIENT_KEY", "PATIENT_KEY",
			new ColumnSpec(schemaName, "ENCOUNTER",
			new JoinSpec("ENCOUNTER_KEY",
			"ENCOUNTER_KEY",
			new ColumnSpec(
			schemaName,
			"CPT_EVENT"))))),
			new ColumnSpec[]{
				new ColumnSpec(schemaName, "CPT_EVENT",
				"EVENT_KEY")},
			new ColumnSpec(schemaName, "CPT_EVENT", "TS_OBX"), null,
			new PropertySpec[]{
				new PropertySpec("code", null,
				new ColumnSpec(schemaName, "CPT_EVENT",
				"ENTITY_ID"),
				ValueType.NOMINALVALUE)}, 
			new ReferenceSpec[]{
				new ReferenceSpec("encounter", "Encounters", new ColumnSpec[]{new ColumnSpec(schemaName, "CPT_EVENT", "ENCOUNTER_KEY")}, ReferenceSpec.Type.ONE)
			}, null,
			new ColumnSpec(schemaName, "CPT_EVENT", "ENTITY_ID",
			Operator.EQUAL_TO, cptMappings, true),
			null, null, null, AbsoluteTimeGranularity.MINUTE,
			dtPositionParser, null),
			new EntitySpec("Medication Orders", null,
			medsMappings.readTargets(),
			true, new ColumnSpec(keyIdSchema, keyIdTable,
			keyIdColumn,
			new JoinSpec("PATIENT_KEY", "PATIENT_KEY",
			new ColumnSpec(schemaName, "ENCOUNTER",
			new JoinSpec("ENCOUNTER_KEY",
			"ENCOUNTER_KEY",
			new ColumnSpec(schemaName,
			"MEDS_EVENT"))))),
			new ColumnSpec[]{
				new ColumnSpec(schemaName, "MEDS_EVENT",
				"EVENT_KEY")},
			new ColumnSpec(schemaName, "MEDS_EVENT", "TS_OBX"),
			null, new PropertySpec[]{ new PropertySpec("code", null,
				new ColumnSpec(schemaName, "MEDS_EVENT",
				"ENTITY_ID"),
				ValueType.NOMINALVALUE),/*new PropertySpec("orderDescription", null, new ColumnSpec(schemaName, "fact_history_medication", new JoinSpec("synonym_order_key", "synonym_order_key", new ColumnSpec(schemaName, "lkp_synonym_order", "synonym_order_desc"))), ValueType.NOMINALVALUE), new PropertySpec("orderContext", null, new ColumnSpec(schemaName, "fact_history_medication", new JoinSpec("context_medication_key", "context_medication_key", new ColumnSpec(schemaName, "lkp_context_medication", "context_medication_id", ColumnSpec.Operator.EQUAL_TO, this.mapper.read("order_context_03292011.txt"), true))), ValueType.NOMINALVALUE), new PropertySpec("continuingOrder", null, new ColumnSpec(schemaName, "fact_history_medication", "order_continuing_ind"), ValueType.BOOLEANVALUE), new PropertySpec("orderStatus", null, new ColumnSpec(schemaName, "fact_history_medication", new JoinSpec("order_status_key", "order_status_key", new ColumnSpec(schemaName, "lkp_order_status", "order_status_id", ColumnSpec.Operator.EQUAL_TO, this.mapper.read("order_status_03292011.txt"), true))), ValueType.NOMINALVALUE), new PropertySpec("orderAction", null, new ColumnSpec(schemaName, "fact_history_medication", new JoinSpec("action_order_key", "action_order_key", new ColumnSpec(schemaName, "lkp_action_order", "action_order_id", ColumnSpec.Operator.EQUAL_TO, this.mapper.read("order_action_03292011.txt"), false))), ValueType.NOMINALVALUE)*/},
			new ReferenceSpec[]{
				new ReferenceSpec("encounter", "Encounters", new ColumnSpec[]{new ColumnSpec(schemaName, "MEDS_EVENT", "ENCOUNTER_KEY")}, ReferenceSpec.Type.ONE)
			}, null,
			new ColumnSpec(schemaName, "MEDS_EVENT", "ENTITY_ID",
			Operator.EQUAL_TO, medsMappings, true), null, null,
			null, AbsoluteTimeGranularity.MINUTE, dtPositionParser,
			null),};
		return eventSpecs;
	}

	@Override
	protected EntitySpec[] primitiveParameterSpecs(String keyIdSchema, String keyIdTable, String keyIdColumn, String keyIdJoinKey) throws IOException {
		String schemaName = getSchemaName();
		Mappings labsMappings = getMappingsFactory().getInstance("labs_08172011.txt");
		Mappings vitalsMappings = getMappingsFactory().getInstance("vitals_result_types_08172011.txt");
		EntitySpec[] primitiveParameterSpecs = new EntitySpec[]{
			new EntitySpec("Labs", null,
			labsMappings.readTargets(),
			true, new ColumnSpec(keyIdSchema, keyIdTable,
			keyIdColumn,
			new JoinSpec("PATIENT_KEY", "PATIENT_KEY",
			new ColumnSpec(schemaName, "ENCOUNTER",
			new JoinSpec("ENCOUNTER_KEY",
			"ENCOUNTER_KEY",
			new ColumnSpec(schemaName,
			"LABS_EVENT"))))),
			new ColumnSpec[]{
				new ColumnSpec(schemaName, "LABS_EVENT",
				"EVENT_KEY")},
			new ColumnSpec(schemaName, "LABS_EVENT", "TS_OBX"),
			null, 
			new PropertySpec[]{
				new PropertySpec("code", null,
				new ColumnSpec(schemaName, "LABS_EVENT",
				"ENTITY_ID"), ValueType.NOMINALVALUE),
				new PropertySpec("unitOfMeasure", null,
				new ColumnSpec(schemaName, "LABS_EVENT",
				"UNITS"), ValueType.NOMINALVALUE),
				/*new PropertySpec("referenceRangeLow", null, new ColumnSpec(schemaName, "fact_result_lab", "reference_range_low_val"), ValueType.NUMBERVALUE), new PropertySpec("referenceRangeHigh", null, new ColumnSpec(schemaName, "fact_result_lab", "reference_range_high_val"), ValueType.NUMBERVALUE), */
				new PropertySpec("interpretation", null,
				new ColumnSpec(schemaName, "LABS_EVENT",
				"FLAG"), ValueType.NOMINALVALUE)}, 
			new ReferenceSpec[]{
				new ReferenceSpec("encounter", "Encounters", new ColumnSpec[]{new ColumnSpec(schemaName, "LABS_EVENT", "ENCOUNTER_KEY")}, ReferenceSpec.Type.ONE)
			},
			null,
			new ColumnSpec(schemaName, "LABS_EVENT", "ENTITY_ID",
			Operator.EQUAL_TO, labsMappings, true), null,
			new ColumnSpec(schemaName, "LABS_EVENT", "RESULT_STR"),
			ValueType.VALUE, AbsoluteTimeGranularity.MINUTE,
			dtPositionParser, null), new EntitySpec("Vitals", null,
			vitalsMappings.readTargets(),
			true, new ColumnSpec(keyIdSchema, keyIdTable,
			keyIdColumn, new JoinSpec("PATIENT_KEY", "PATIENT_KEY",
			new ColumnSpec(schemaName, "ENCOUNTER",
			new JoinSpec("ENCOUNTER_KEY", "ENCOUNTER_KEY",
			new ColumnSpec(schemaName, "VITALS_EVENT"))))),
			new ColumnSpec[]{
				new ColumnSpec(schemaName, "VITALS_EVENT",
				"EVENT_KEY")},
			new ColumnSpec(schemaName, "VITALS_EVENT", "TS_OBX"), null,
			new PropertySpec[]{
				new PropertySpec("unitOfMeasure", null,
				new ColumnSpec(schemaName, "VITALS_EVENT",
				"UNITS"), ValueType.NOMINALVALUE),
				new PropertySpec("interpretation", null,
				new ColumnSpec(schemaName, "VITALS_EVENT",
				"FLAG"), ValueType.NOMINALVALUE)}, 
			new ReferenceSpec[]{
				new ReferenceSpec("encounter", "Encounters", new ColumnSpec[]{new ColumnSpec(schemaName, "VITALS_EVENT", "ENCOUNTER_KEY")}, ReferenceSpec.Type.ONE)
			},
			null, new ColumnSpec(schemaName, "VITALS_EVENT", "ENTITY_ID",
			Operator.EQUAL_TO, vitalsMappings, true), null,
			new ColumnSpec(schemaName, "VITALS_EVENT", "RESULT_STR"),
			ValueType.VALUE, AbsoluteTimeGranularity.MINUTE,
			dtPositionParser, null),};
		return primitiveParameterSpecs;
	}

	@Override
	public void close() throws BackendCloseException {
		super.close();
		BackendCloseException exceptionToThrow = null;
		try {
			SQLExecutor.executeSQL(getConnectionSpecInstance(), "DROP ALL OBJECTS", null);
		} catch (SQLException | InvalidConnectionSpecArguments ex) {
			exceptionToThrow = new BackendCloseException("Error in data source backend " + nameForErrors() + ": could not drop the database", ex);
		}
		if (dataProviders != null) {
			for (XlsxDataProvider dataProvider : dataProviders) {
				try {
					dataProvider.close();
				} catch (IOException ex) {
					if (exceptionToThrow == null) {
						exceptionToThrow = new BackendCloseException("Error in data source backend " + nameForErrors() + ": could not close Excel spreadsheet " + dataProvider.getDataFile().getName(), ex);
					}
				}
			}
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
		}
	}
}
