package com.thed.zephyr.jenkins.model;

import java.util.List;
import java.util.Set;

public class ZephyrConfigModel {

	private List<TestCaseResultModel> testcases;
	private Set<String> packageNames;
	private String userName;
	private String password;
	private String cyclePrefix;
	private String zephyrURL;
	private String cycleDuration;
	private long zephyrProjectId;
	private long releaseId;
	private long cycleId;
	private long userId;
	private boolean createPackage;

	public String getZephyrURL() {
		return zephyrURL;
	}

	public void setZephyrURL(String zephyrURL) {
		this.zephyrURL = zephyrURL;
	}

	public boolean isCreatePackage() {
		return createPackage;
	}

	public void setCreatePackage(boolean createPackage) {
		this.createPackage = createPackage;
	}

	public String getCyclePrefix() {
		return cyclePrefix;
	}

	public void setCyclePrefix(String cyclePrefix) {
		this.cyclePrefix = cyclePrefix;
	}

	public List<TestCaseResultModel> getTestcases() {
		return testcases;
	}

	public void setTestcases(List<TestCaseResultModel> testcases) {
		this.testcases = testcases;
	}

	public long getZephyrProjectId() {
		return zephyrProjectId;
	}

	public void setZephyrProjectId(long zephyrProjectId) {
		this.zephyrProjectId = zephyrProjectId;
	}

	public long getReleaseId() {
		return releaseId;
	}

	public void setReleaseId(long releaseId) {
		this.releaseId = releaseId;
	}

	public long getCycleId() {
		return cycleId;
	}

	public void setCycleId(long cycleId) {
		this.cycleId = cycleId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Set<String> getPackageNames() {
		return packageNames;
	}

	public void setPackageNames(Set<String> packageNames) {
		this.packageNames = packageNames;
	}

	public String getCycleDuration() {
		return cycleDuration;
	}

	public void setCycleDuration(String cycleDuration) {
		this.cycleDuration = cycleDuration;
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

}