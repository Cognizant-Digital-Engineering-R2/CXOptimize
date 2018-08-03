/*
 * Copyright 2014 - 2018 Cognizant Technology Solutions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cognizant.pace.CXOptimize.api.utils;


import com.cognizant.pace.CXOptimize.AnalysisEngine.GlobalConstants;
import com.cognizant.pace.CXOptimize.AnalysisEngine.PaceAnalysisEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;

@Component
public class CXOptimizeStarter
{

	@Autowired
	private SubscriptionValidator subscriptionValidator;

	@Value("${elasticsearch.config.index}")
	private String configIndex;

	@Value("${beacon.host}")
	private String beaconHost;

	@Value("${dashboard.placeholder}")
	private String dashboardPlaceholder;

	@Value("${dashboard.name}")
	private String dashboardName;

	@Value("${scenario.placeholder}")
	private String scenarioPlaceholder;

	@Value("${project.placeholder}")
	private String projectPlaceholder;

	@Value("${client.placeholder}")
	private String clientPlaceholder;

	@Value("${dashboard.link}")
	private String dashboardLink;

	@Value("${default.config}")
	private String defaultConfig;

	@Value("${mark.mapping.singlenode}")
	private String markMappingSingleNode;

	@Value("${mark.mapping.multinode}")
	private String markMappingMultiNode;



	@Value("${auditlog.mapping.singlenode}")
	private String auditMappingSingleNode;

	@Value("${auditlog.mapping.multinode}")
	private String auditMappingMultiNode;

	@Value("${analysis.mapping.singlenode}")
	private String analysisMappingSingleNode;

	@Value("${analysis.mapping.multinode}")
	private String analysisMappingMultiNode;

	@Value("${baseline.mapping.singlenode}")
	private String baselineMappingSingleNode;

	@Value("${baseline.mapping.multinode}")
	private String baselineMappingMultiNode;

	@Value("${stats.mapping.singlenode}")
	private String statsMappingSingleNode;

	@Value("${stats.mapping.multinode}")
	private String statsMappingMultiNode;

	@Value("${config.mapping.singlenode}")
	private String configMappingSingleNode;

	@Value("${config.mapping.multinode}")
	private String configMappingMultiNode;


	@Value("${userdetails.index.singlenode}")
	private String userdetailsMappingSingleNode;

	@Value("${userdetails.index.multinode}")
	private String userdetailsMappingMultiNode;

	@Value("${elasticsearch.default.runstats.index}")
	private String statsIndex;

	@Value("${elasticsearch.analysis.index}")
	private String analysisIndex;


	@Value("${elasticsearch.user.index}")
	private String userIndex;

	@Value("${elasticsearch.baseline.index}")
	private String baselineIndex;

	@Value("${elasticsearch.url.default}")
	private String elasticUrl;

	@Value("${elasticsearch.config.type}")
	private String configType;

	@Value("${rules.doc}")
	private String rules;

	@Value("${elasticsearch.rules.type}")
	private String rulesType;

	@Value("${server.port}")
	private String portNum;

	@Value("${kibana.statsindex}")
	private String kibanastatsindex;

	@Value("${kibana.url}")
	private String kibanaUrl;

	@Value("${kibana.version.details}")
	private String kibanaconfig;

	@Value("${kibana.version}")
	private String kibanaVersion;

	@Value("${kibana.mapping}")
	private String kibanaMapping;

	@Value("${elasticsearch.mark.index}")
	private String markIndex;

	@Value("${elasticsearch.audit.index}")
	private String auditIndex;

	@Value("${kibana.httpcalls.trend}")
	private String httpCallsTrend;
	@Value("${kibana.payload.trend}")
	private String httpPayloadTrend;
	@Value("${kibana.clienttime.transaction.breakdown}")
	private String clienttimeTransactionBreakdown;
	@Value("${kibana.test.execution.summary}")
	private String executiveSummary;
	@Value("${kibana.servertime.transaction.breakdown}")
	private String servertimeTransactionBreakdown;
	@Value("${kibana.transaction.breakdown}")
	private String transactionBreakdown;
	@Value("${kibana.transactionlevel.responsetime}")
	private String transactionlevelRT;
	@Value("${kibana.total.pageload.time}")
	private String totalPageloadtime;

	@Value("${kibana.cxoptimize.dashboard}")
	private String cxoptimize;


	@Value("${customrules.folder.name}")
	private String rulesFolder;

	@Value("${jwt.token.key}")
	private String jwtKey;

	@Value("${jwt.token.expiry.in.minutes}")
	private long tokenExpiry;

	@Value("${secret.key}")
	private String secretKey;

	@Value("${runids.to.display}")
	private int runsToDisplay;

	private static final Logger LOGGER = LoggerFactory.getLogger(CXOptimizeStarter.class);


	@PostConstruct
	public void indexCheckOrCreate() throws Exception {
		GlobalConstants.setESUrl(elasticUrl);

		GlobalConstants.setJWTKey(jwtKey);

		GlobalConstants.setExpirationTime(tokenExpiry);

		GlobalConstants.setRunIDCount(runsToDisplay);

		GlobalConstants.setKey(secretKey);


		LOGGER.info("Data store url {}", GlobalConstants.getESUrl());

		LOGGER.info("API token expiry time {}", GlobalConstants.getExpirationTime());


		LOGGER.info("Check for availability of all index required for cxoptimize");
		GlobalConstants.setNodeCount(PaceAnalysisEngine.getNumberOfNodes(elasticUrl));
		LOGGER.info("Number of nodes in Elastic Search Cluster is {}", GlobalConstants.getNodeCount());

		boolean checkFlag = PaceAnalysisEngine.checkOrCreateIndex(elasticUrl, configIndex, GlobalConstants.getNodeCount() == 1 ? configMappingSingleNode : configMappingMultiNode);
		if (checkFlag) {
			LOGGER.info("Config Index available or created successfully");
		} else {
			LOGGER.info("Config Index not available.Check if datasource is up and running and properties file is placed in project path");
			System.exit(1);
		}

		checkFlag = PaceAnalysisEngine.setupRules(elasticUrl, rules);
		if (checkFlag) {
			LOGGER.info("Default rules available or created successfully");
		} else {
			LOGGER.info("Default rules not available.Check if datasource is up and running and properties file is placed in project path");
			System.exit(1);
		}


		checkFlag = PaceAnalysisEngine.checkOrCreateIndex(elasticUrl, statsIndex, GlobalConstants.getNodeCount() == 1 ? statsMappingSingleNode : statsMappingMultiNode);
		if (checkFlag) {
			LOGGER.info("Stats Index available or created successfully");
		} else {
			LOGGER.info("Stats Index not available.Check if datasource is up and running and properties file is placed in project path");
			System.exit(1);
		}

		checkFlag = PaceAnalysisEngine.checkOrCreateIndex(elasticUrl, analysisIndex, GlobalConstants.getNodeCount() == 1 ? analysisMappingSingleNode : analysisMappingMultiNode);
		if (checkFlag) {
			LOGGER.info("Analysis Index available or created successfully");
		} else {
			LOGGER.info("Analysis Index not available.Check if datasource is up and running and properties file is placed in project path");
			System.exit(1);
		}

		checkFlag = PaceAnalysisEngine.checkOrCreateIndex(elasticUrl, baselineIndex, GlobalConstants.getNodeCount() == 1 ? baselineMappingSingleNode : baselineMappingMultiNode);
		if (checkFlag) {
			LOGGER.info("Baseline Index available or created successfully");
		} else {
			LOGGER.info("Baseline Index not available.Check if datasource is up and running and properties file is placed in project path");
			System.exit(1);
		}

		checkFlag = PaceAnalysisEngine.checkOrCreateIndex(elasticUrl, markIndex, GlobalConstants.getNodeCount() == 1 ? markMappingSingleNode : markMappingMultiNode);
		if (checkFlag) {
			LOGGER.info("Mark Index available or created successfully");
		} else {
			LOGGER.info("Mark Index not available.Check if datasource is up and running and properties file is placed in project path");
			System.exit(1);
		}

		checkFlag = PaceAnalysisEngine.checkOrCreateIndex(elasticUrl, auditIndex, GlobalConstants.getNodeCount() == 1 ? auditMappingSingleNode : auditMappingMultiNode);
		if (checkFlag) {
			LOGGER.info("AuditLog Index available or created successfully");
		} else {
			LOGGER.info("AuditLog Index not available.Check if datasource is up and running and properties file is placed in project path");
			System.exit(1);
		}

		checkFlag = PaceAnalysisEngine.checkOrCreateIndex(elasticUrl, userIndex, GlobalConstants.getNodeCount() == 1 ? userdetailsMappingSingleNode : userdetailsMappingMultiNode);
		if (checkFlag) {
			LOGGER.info("User Index available or created successfully");
			boolean userFlag = PaceAnalysisEngine.createStandardUsers(elasticUrl);
			if (userFlag) {
				LOGGER.info("Standard Users created successfully");
			} else {
				LOGGER.info("Standard Users exists");
			}
		} else {
			LOGGER.info("User Index not available.Check if datasource is up and running and properties file is placed in project path");
			System.exit(1);
		}


		checkFlag = PaceAnalysisEngine.checkOrCreateIndex(elasticUrl, ".kibana", kibanaMapping);
		if (checkFlag) {
			LOGGER.info("Kibana Index available or created successfully");
		} else {
			LOGGER.info("Kibana Index not available.Check if datasource is up and running and properties file is placed in project path");
			System.exit(1);
		}

		//Update Kibana version to Kibana index
		checkFlag = PaceAnalysisEngine.setupKibanaDashboard(elasticUrl, ".kibana/index-pattern/" + statsIndex, kibanastatsindex);
		if (checkFlag) {
			LOGGER.info("Kibana index pattern doc exists or created successfully");
		} else {
			LOGGER.info("Problem creating Kibana document");
		}

		checkFlag = PaceAnalysisEngine.setupKibanaDashboard(elasticUrl, ".kibana/config/" + kibanaVersion, kibanaconfig);
		if (checkFlag) {
			LOGGER.info("Kibana config updated successfully");
		} else {
			LOGGER.info("Problem updating Kibana config");
		}

		//Create Visualization


		checkFlag = PaceAnalysisEngine.setupKibanaDashboard(elasticUrl, ".kibana/visualization/HTTP-Calls-Trending", httpCallsTrend);
		if (checkFlag) {
			LOGGER.info("HTTP-Calls-Trending doc exists or created successfully");
		} else {
			LOGGER.info("Problem creating HTTP-Calls-Trending doc");
		}

		checkFlag = PaceAnalysisEngine.setupKibanaDashboard(elasticUrl, ".kibana/visualization/Payload-Trending", httpPayloadTrend);
		if (checkFlag) {
			LOGGER.info("Payload-Trending doc exists or created successfully");
		} else {
			LOGGER.info("Problem creating Payload-Trending doc");
		}


		checkFlag = PaceAnalysisEngine.setupKibanaDashboard(elasticUrl, ".kibana/visualization/Client-Time-Transaction-BreakDown", clienttimeTransactionBreakdown);
		if (checkFlag) {
			LOGGER.info("Client-Time-Transaction-BreakDown doc exists or created successfully");
		} else {
			LOGGER.info("Problem creating Client-Time-Transaction-BreakDown doc");
		}

		checkFlag = PaceAnalysisEngine.setupKibanaDashboard(elasticUrl, ".kibana/visualization/Test-Execution-Summary-Board", executiveSummary);
		if (checkFlag) {
			LOGGER.info("Test-Execution-Summary-Board doc exists or created successfully");
		} else {
			LOGGER.info("Problem creating Test-Execution-Summary-Board doc");
		}

		checkFlag = PaceAnalysisEngine.setupKibanaDashboard(elasticUrl, ".kibana/visualization/Server-Time-Transaction-BreakDown", servertimeTransactionBreakdown);
		if (checkFlag) {
			LOGGER.info("Server-Time-Transaction-BreakDown doc exists or created successfully");
		} else {
			LOGGER.info("Problem creating Server-Time-Transaction-BreakDown doc");
		}

		checkFlag = PaceAnalysisEngine.setupKibanaDashboard(elasticUrl, ".kibana/visualization/Transaction-Break-Down", transactionBreakdown);
		if (checkFlag) {
			LOGGER.info("Transaction-Break-Down doc exists or created successfully");
		} else {
			LOGGER.info("Problem creating Transaction-Break-Down doc");
		}

		checkFlag = PaceAnalysisEngine.setupKibanaDashboard(elasticUrl, ".kibana/visualization/Transaction-level-Response-times-Comparison", transactionlevelRT);
		if (checkFlag) {
			LOGGER.info("Transaction-level-Response-times-Comparison doc exists or created successfully");
		} else {
			LOGGER.info("Problem creating Transaction-level-Response-times-Comparison doc");
		}

		checkFlag = PaceAnalysisEngine.setupKibanaDashboard(elasticUrl, ".kibana/visualization/Total-Page-Load-Time-Trending", totalPageloadtime);
		if (checkFlag) {
			LOGGER.info("Total-Page-Load-Time-Trending doc exists or created successfully");
		} else {
			LOGGER.info("Problem creating Total-Page-Load-Time-Trending doc");
		}

		checkFlag = PaceAnalysisEngine.setupKibanaDashboard(elasticUrl, ".kibana/dashboard/" + dashboardName, cxoptimize.replace(dashboardPlaceholder, dashboardName));
		if (checkFlag) {
			LOGGER.info("Kibana CXOptimize_Dashboard doc exists or created successfully");
		} else {
			LOGGER.info("Problem creating CXOptimize_Dashboard doc");
		}


		LOGGER.info("Completed Kibana dashboard setup");

			LOGGER.info("Creating folder for custom rules");
			File theDir = new File("./" + rulesFolder.trim());
			if (!theDir.exists()) {
				LOGGER.info("Creating Rules directory ");
				boolean result = false;
				try {
					theDir.mkdir();
					result = true;
				} catch (SecurityException se) {
					LOGGER.error(se.getMessage());
				}
				if (result) {
					LOGGER.info("Rules directory created");
				}
			} else {
				LOGGER.info("Rules directory already Exists");
			}

	}

}
