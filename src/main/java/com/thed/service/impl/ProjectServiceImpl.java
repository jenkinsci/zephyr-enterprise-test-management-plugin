package com.thed.service.impl;

import com.thed.model.Project;
import com.thed.model.ProjectTeam;
import com.thed.model.User;
import com.thed.service.ProjectService;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by prashant on 25/6/19.
 */
public class ProjectServiceImpl extends BaseServiceImpl implements ProjectService {

    public ProjectServiceImpl() {
        super();
    }

    @Override
    public List<Project> getAllProjectsForCurrentUser() throws URISyntaxException {
        List<Project> projects = zephyrRestService.getAllProjectsForCurrentUser();
        User user = zephyrRestService.getCurrentUser();

        List<Project> userProjects = new ArrayList<>();

        projectLoop : for (Project project : projects) {
            List<ProjectTeam> projectTeams = project.getMembers();

            if(projectTeams == null) {
                continue;
            }

            for (ProjectTeam projectTeam : projectTeams) {
                if(projectTeam.getUserId().equals(user.getId())) {
                    userProjects.add(project);
                    continue projectLoop;
                }
            }
        }

        return userProjects;
    }

    @Override
    public Project getProjectById(Long projectId) throws URISyntaxException {
        return zephyrRestService.getProjectById(projectId);
    }

    @Override
    public Long getProjectDurationInDays(Long projectId) throws URISyntaxException {
        Project project = getProjectById(projectId);

        if(project == null || project.getEndDate() == null) {
            return -1l;
        }

        return getDateDiff(project.getStartDate(), project.getEndDate(), TimeUnit.DAYS);
    }

    /**
     * Get a diff between two dates
     * @param date1 the oldest date
     * @param date2 the newest date
     * @param timeUnit the unit in which you want the diff
     * @return the diff value, in the provided unit
     */
    private long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS);
    }
}
