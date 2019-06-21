package com.thed.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by prashant on 21/6/19.
 */
public class Cycle extends BaseEntity {

    private final static String DATE_FORMAT = "MM/dd/yyyy";

    private Long id;
    private String name;
    private Long releaseId;
    private Long startDate;
    private Long endDate;
    private String cycleStartDate;
    private String cycleEndDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getReleaseId() {
        return releaseId;
    }

    public void setReleaseId(Long releaseId) {
        this.releaseId = releaseId;
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
        setCycleStartDate(dateFormat.format(startDate));
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
        setCycleEndDate(dateFormat.format(endDate));
    }

    public String getCycleStartDate() {
        return cycleStartDate;
    }

    public void setCycleStartDate(String cycleStartDate) {
        this.cycleStartDate = cycleStartDate;
    }

    public String getCycleEndDate() {
        return cycleEndDate;
    }

    public void setCycleEndDate(String cycleEndDate) {
        this.cycleEndDate = cycleEndDate;
    }
}
