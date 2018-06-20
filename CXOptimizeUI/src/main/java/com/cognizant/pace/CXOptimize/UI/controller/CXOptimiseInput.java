package com.cognizant.pace.CXOptimize.UI.controller;

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

public class CXOptimiseInput
{
    private String clientName;
    private String projectName;
    private String scenarioName;
    private String analysisType;
    private String currentRun;
    private String baselineRun;
    private String Override;
    private String scale;
    private String currentStartDate;
    private String currentEndDate;
    private String baselineStartDate;
    private String baselineEndDate;

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName.replaceAll("[^a-zA-Z0-9]","");
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName.replaceAll("[^a-zA-Z0-9]","");
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName.replaceAll("[^a-zA-Z0-9]","");
    }

    public String getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType(String analysisType) {
        this.analysisType = analysisType.replaceAll("[^a-zA-Z]","");
    }

    public String getCurrentRun() {
        return currentRun;
    }

    public void setCurrentRun(String currentRun) {
        this.currentRun = currentRun.replaceAll("[^0-9]","");
    }

    public String getBaselineRun() {
        return baselineRun;
    }

    public void setBaselineRun(String baselineRun) {
        this.baselineRun = baselineRun.replaceAll("[^0-9]","");
    }

    public String getOverride() {
        return Override;
    }

    public void setOverride(String override) {
        this.Override = override;
    }

    public String getScale() {
        return scale;
    }

    public void setScale(String lscale) {
        this.scale = lscale;
    }
    public String getCurrentStartDate() {
        return currentStartDate;
    }

    public void setCurrentStartDate(String lcurrentStartDate) {
        this.currentStartDate = lcurrentStartDate;
    }

    public String getCurrentEndDate() {
        return currentEndDate;
    }

    public void setCurrentEndDate(String lcurrentEndDate) {
        this.currentEndDate = lcurrentEndDate;
    }
    public String getBaselineStartDate() {
        return baselineStartDate;
    }

    public void setBaselineStartDate(String lbaselineStartDate) {
        this.baselineStartDate = lbaselineStartDate;
    }

    public String getBaselineEndDate() {
        return baselineEndDate;
    }

    public void setBaselineEndDate(String lbaselineEndDate) {
        this.baselineEndDate = lbaselineEndDate;
    }
}
