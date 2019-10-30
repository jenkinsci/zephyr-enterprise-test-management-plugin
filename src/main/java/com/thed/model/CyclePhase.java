package com.thed.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by prashant on 28/6/19.
 */
public class CyclePhase extends BaseEntity {

    private final static String DATE_FORMAT = "MM/dd/yyyy";

    private String name;
    private Long cycleId;
    private Long releaseId;
    private Boolean freeForm;
    private Long tcrCatalogTreeId;
    private Long startDate;
    private Long endDate;
    private String phaseStartDate;
    private String phaseEndDate;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCycleId() {
        return cycleId;
    }

    public void setCycleId(Long cycleId) {
        this.cycleId = cycleId;
    }

    public Long getTcrCatalogTreeId() {
        return tcrCatalogTreeId;
    }

    public void setTcrCatalogTreeId(Long tcrCatalogTreeId) {
        this.tcrCatalogTreeId = tcrCatalogTreeId;
    }

    public Long getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        this.startDate = calendar.getTimeInMillis();
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        setPhaseStartDate(dateFormat.format(startDate));
    }

    public Long getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(endDate);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        this.endDate = calendar.getTimeInMillis();
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        setPhaseEndDate(dateFormat.format(endDate));
    }

    public String getPhaseStartDate() {
        return phaseStartDate;
    }

    public void setPhaseStartDate(String phaseStartDate) {
        this.phaseStartDate = phaseStartDate;
    }

    public String getPhaseEndDate() {
        return phaseEndDate;
    }

    public void setPhaseEndDate(String phaseEndDate) {
        this.phaseEndDate = phaseEndDate;
    }

    public Long getReleaseId() {
        return releaseId;
    }

    public void setReleaseId(Long releaseId) {
        this.releaseId = releaseId;
    }

    public Boolean getFreeForm() {
        return freeForm;
    }

    public void setFreeForm(Boolean freeForm) {
        this.freeForm = freeForm;
    }
}
