package com.thed.zephyr.jenkins.model;

import java.util.ArrayList;
import java.util.List;

public class Testcase {

	private String name;
	private String description = "";
	private boolean isComplex = false;
	private int estimatedTime = 600;
	private int writerId = 0;
	private int lastUpdaterId = 0;
	private int oldId = 0;
	private boolean automated = false;
	private List<Object> requirementIds = new ArrayList<>();
	private int attachmentCount = 0;
	private long releaseId;
	private boolean automatedDefault = false;
	private Object customProperties = null;
	private long projectId;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean getIsComplex() {
		return isComplex;
	}

	public void setIsComplex(boolean isComplex) {
		this.isComplex = isComplex;
	}

	public int getEstimatedTime() {
		return estimatedTime;
	}

	public void setEstimatedTime(int estimatedTime) {
		this.estimatedTime = estimatedTime;
	}

	public int getWriterId() {
		return writerId;
	}

	public void setWriterId(int writerId) {
		this.writerId = writerId;
	}

	public int getLastUpdaterId() {
		return lastUpdaterId;
	}

	public void setLastUpdaterId(int lastUpdaterId) {
		this.lastUpdaterId = lastUpdaterId;
	}

	public int getOldId() {
		return oldId;
	}

	public void setOldId(int oldId) {
		this.oldId = oldId;
	}

	public boolean getAutomated() {
		return automated;
	}

	public void setAutomated(boolean automated) {
		this.automated = automated;
	}

	public List<Object> getRequirementIds() {
		return requirementIds;
	}

	public void setRequirementIds(List<Object> requirementIds) {
		this.requirementIds = requirementIds;
	}

	public int getAttachmentCount() {
		return attachmentCount;
	}

	public void setAttachmentCount(int attachmentCount) {
		this.attachmentCount = attachmentCount;
	}

	public long getReleaseId() {
		return releaseId;
	}

	public void setReleaseId(long releaseId) {
		this.releaseId = releaseId;
	}

	public boolean getAutomatedDefault() {
		return automatedDefault;
	}

	public void setAutomatedDefault(boolean automatedDefault) {
		this.automatedDefault = automatedDefault;
	}

	public Object getCustomProperties() {
		return customProperties;
	}

	public void setCustomProperties(Object customProperties) {
		this.customProperties = customProperties;
	}

	public long getProjectId() {
		return projectId;
	}

	public void setProjectId(long projectId2) {
		this.projectId = projectId2;
	}

}