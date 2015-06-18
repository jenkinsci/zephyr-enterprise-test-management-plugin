package com.getzephyr.jenkins.model;

import com.thed.service.soap.RemoteTestcase;

public class TestCaseResultModel {

	private RemoteTestcase remoteTestcase;
	private Boolean passed;

	public RemoteTestcase getRemoteTestcase() {
		return remoteTestcase;
	}

	public void setRemoteTestcase(RemoteTestcase remoteTestcase) {
		this.remoteTestcase = remoteTestcase;
	}

	public Boolean getPassed() {
		return passed;
	}

	public void setPassed(Boolean passed) {
		this.passed = passed;
	}

}
