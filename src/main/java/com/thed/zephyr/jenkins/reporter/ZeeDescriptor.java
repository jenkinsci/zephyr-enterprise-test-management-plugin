package com.thed.zephyr.jenkins.reporter;

import static com.thed.zephyr.jenkins.reporter.ZeeConstants.ADD_ZEPHYR_GLOBAL_CONFIG;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.CYCLE_DURATION_1_DAY;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.CYCLE_DURATION_30_DAYS;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.CYCLE_DURATION_7_DAYS;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.NAME_POST_BUILD_ACTION;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.NEW_CYCLE_KEY;

import com.thed.service.*;
import com.thed.service.impl.*;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.thed.zephyr.jenkins.model.ZephyrInstance;
import com.thed.zephyr.jenkins.utils.URLValidator;
import org.kohsuke.stapler.verb.POST;

@Symbol("zeeReporter")
@Extension
public class ZeeDescriptor extends BuildStepDescriptor<Publisher> {

	private static Logger logger = Logger.getLogger(ZeeDescriptor.class.getName());

    private UserService userService = new UserServiceImpl();
    private ProjectService projectService = new ProjectServiceImpl();
    private ReleaseService releaseService = new ReleaseServiceImpl();
    private CycleService cycleService = new CycleServiceImpl();

	private List<ZephyrInstance> zephyrInstances;

	public List<ZephyrInstance> getZephyrInstances() {
		return zephyrInstances;
	}

	public void setZephyrInstances(List<ZephyrInstance> zephyrInstances) {
		this.zephyrInstances = zephyrInstances;
	}

	public ZeeDescriptor() {
		super(ZeeReporter.class);
		load();
	}

	@Override
	public Publisher newInstance(StaplerRequest req, JSONObject formData)
			throws FormException {
		return super.newInstance(req, formData);
	}

	@Override
	public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
		return true;
	}

	@Override
	public boolean configure(StaplerRequest req, JSONObject formData)
			throws FormException {
		req.bindParameters(this);
		
		logger.info("Displaying Zephyr server config section");
		
		this.zephyrInstances = new ArrayList<ZephyrInstance>();
		Object object = formData.get("zephyrInstances");
		if (object instanceof JSONArray) {
			JSONArray jArr = (JSONArray) object;
			for (Iterator iterator = jArr.iterator(); iterator.hasNext();) {
				JSONObject jObj = (JSONObject) iterator.next();
				verifyCredentials(jObj);
			}

		} else if (object instanceof JSONObject) {
			JSONObject jObj = formData.getJSONObject("zephyrInstances");
            verifyCredentials(jObj);
		}
		save();
		return super.configure(req, formData);
	}

    /**
     * Verifies credentials present in jsonObject
     * @param jObj
     */
    private void verifyCredentials(JSONObject jObj) {
        ZephyrInstance zephyrInstance = new ZephyrInstance();

        try {
            String server = URLValidator.validateURL(jObj.getString("serverAddress").trim());
            String user = jObj.getString("username").trim();
            String pass = jObj.getString("password").trim();

            zephyrInstance.setServerAddress(server);
            zephyrInstance.setUsername(user);
            zephyrInstance.setPassword(pass);

            boolean zephyrServerValidation = userService.verifyCredentials(server, user, pass);
            if (zephyrServerValidation) {
                this.zephyrInstances.add(zephyrInstance);
            }
        } catch (Throwable e) {
            logger.log(Level.ALL, "Error in validating server and credentials. ");
            logger.log(Level.ALL, e.getMessage());
        }
    }

	@Override
	public String getDisplayName() {
		return NAME_POST_BUILD_ACTION;
	}

    @POST
	public FormValidation doTestConnection(
			@QueryParameter String serverAddress,
			@QueryParameter String username, @QueryParameter String password) {

        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);

		if (StringUtils.isBlank(serverAddress)) {
			return FormValidation.error("Please enter the server name");
		}

		if (StringUtils.isBlank(username)) {
			return FormValidation.error("Please enter the username");
		}

		if (StringUtils.isBlank(password)) {
			return FormValidation.error("Please enter the password");
		}

		if (!(serverAddress.trim().startsWith("https://") || serverAddress
				.trim().startsWith("http://"))) {
			return FormValidation.error("Incorrect server address format");
		}

		String zephyrURL = URLValidator.validateURL(serverAddress);

        try {
            Boolean verifyCredentials = userService.verifyCredentials(serverAddress, username, password);
            if(!verifyCredentials) {
                return FormValidation.error("Username or password is incorrect.");
            }
        } catch (Exception e) {
            return FormValidation.error("Error occurred while verifying credentials. Please try again later.");
        }

		return FormValidation.ok("Connection to Zephyr has been validated");
	}

	public ListBoxModel doFillServerAddressItems(
			@QueryParameter String serverAddress) {
		return fetchServerList(serverAddress);
	}

	private ListBoxModel fetchServerList(String serverAddress) {
		ListBoxModel m = new ListBoxModel();

		if (this.zephyrInstances.size() > 0) {

			for (ZephyrInstance s : this.zephyrInstances) {
				m.add(s.getServerAddress());
			}
		} else if (StringUtils.isBlank(serverAddress)
				|| serverAddress.trim().equals(ADD_ZEPHYR_GLOBAL_CONFIG)) {
			m.add(ADD_ZEPHYR_GLOBAL_CONFIG);
		} else {
			m.add(ADD_ZEPHYR_GLOBAL_CONFIG);
		}
		return m;
	}

	public ListBoxModel doFillProjectKeyItems(
			@QueryParameter String serverAddress) {
		return fetchProjectList(serverAddress);
	}

	private ListBoxModel fetchProjectList(String serverAddress) {
		ListBoxModel m = new ListBoxModel();

		if (StringUtils.isBlank(serverAddress)) {
	        ListBoxModel mi = fetchServerList(serverAddress);
			serverAddress = mi.get(0).value;
		}
		if (serverAddress.trim().equals(ADD_ZEPHYR_GLOBAL_CONFIG)
				|| (this.zephyrInstances.size() == 0)) {
			m.add(ADD_ZEPHYR_GLOBAL_CONFIG);
			return m;
		}

        try {

            ZephyrInstance zephyrInstance = fetchZephyrInstance(serverAddress);
            userService.login(zephyrInstance.getServerAddress(), zephyrInstance.getUsername(), zephyrInstance.getPassword());

            List<com.thed.model.Project> projects = projectService.getAllProjectsForCurrentUser();
            for (com.thed.model.Project project : projects) {
                if(project.getGlobalProject() == Boolean.FALSE) {
                    m.add(project.getName(), project.getId().toString());
                }
            }
        }
        catch(Exception e) {
            //Todo: handle exceptions gracefully
            e.printStackTrace();
        }

		return m;
	}

	/**
	 * @param serverAddress
	 */
	private ZephyrInstance fetchZephyrInstance(String serverAddress) {
		
		ZephyrInstance zephyrInstance = new ZephyrInstance();
		zephyrInstance.setServerAddress(serverAddress);
		String tempUserName = null;
		String tempPassword = null;

		for (ZephyrInstance z : zephyrInstances) {
			if (z.getServerAddress().trim().equals(serverAddress)) {
				tempUserName = z.getUsername();
				tempPassword = z.getPassword();
			}
		}
		zephyrInstance.setUsername(tempUserName);
		zephyrInstance.setPassword(tempPassword);
		return zephyrInstance;
		
		
	}

	public ListBoxModel doFillReleaseKeyItems(
			@QueryParameter String projectKey,
			@QueryParameter String serverAddress) {

		return fetchReleaseList(projectKey, serverAddress);

	}

	private ListBoxModel fetchReleaseList(String projectKey, String serverAddress) {
		ListBoxModel listBoxModel = new ListBoxModel();

		if (StringUtils.isBlank(serverAddress)) {
	        ListBoxModel mi = fetchServerList(serverAddress);
			serverAddress = mi.get(0).value;
		}
		if (StringUtils.isBlank(projectKey)) {
	        ListBoxModel mi = fetchProjectList(serverAddress);
	        projectKey = mi.get(0).value;
		}

		if (projectKey.trim().equals(ADD_ZEPHYR_GLOBAL_CONFIG)
				|| (this.zephyrInstances.size() == 0)) {
			listBoxModel.add(ADD_ZEPHYR_GLOBAL_CONFIG);
			return listBoxModel;
		}

        try {
            Long projectId = Long.parseLong(projectKey);
            List<com.thed.model.Release> releases = releaseService.getAllReleasesForProjectId(projectId);
            for (com.thed.model.Release release : releases) {
                listBoxModel.add(release.getName(), release.getId().toString());
            }
        }
        catch(Exception e) {
            //todo: handle exception gracefully
            e.printStackTrace();
        }

		return listBoxModel;
	}

	public ListBoxModel doFillCycleKeyItems(@QueryParameter String releaseKey, @QueryParameter String projectKey,
											@QueryParameter String serverAddress) {

		ListBoxModel listBoxModel = new ListBoxModel();
		
		if (StringUtils.isBlank(serverAddress)) {
	        ListBoxModel mi = fetchServerList(serverAddress);
            if(mi.size() == 0) {
                listBoxModel.add(ADD_ZEPHYR_GLOBAL_CONFIG);
                return listBoxModel;
            }
			serverAddress = mi.get(0).value;
		}

        if(StringUtils.isBlank(projectKey)) {
            ListBoxModel mi = fetchProjectList(serverAddress);
            if(mi.size() == 0) {
                return listBoxModel;
            }
            projectKey = mi.get(0).value;
        }

		if (StringUtils.isBlank(releaseKey)) {
	        ListBoxModel mi = fetchReleaseList(projectKey, serverAddress);
            if(mi.size() == 0) {
                return listBoxModel;
            }
	        releaseKey = mi.get(0).value;
		}

		if (releaseKey.trim().equals(ADD_ZEPHYR_GLOBAL_CONFIG)
				|| (this.zephyrInstances.size() == 0)) {
			listBoxModel.add(ADD_ZEPHYR_GLOBAL_CONFIG);
			return listBoxModel;
		}

        try {
            Long releaseId = Long.parseLong(releaseKey);
            List<com.thed.model.Cycle> cycles = cycleService.getAllCyclesForReleaseId(releaseId);
            for (com.thed.model.Cycle cycle : cycles) {
                listBoxModel.add(cycle.getName(), cycle.getId().toString());
            }
        }
        catch(Exception e) {
            //todo: handle exceptions gracefully
            e.printStackTrace();
        }

		listBoxModel.add("New Cycle", NEW_CYCLE_KEY);

		return listBoxModel;
	}

	public ListBoxModel doFillCycleDurationItems(
			@QueryParameter String serverAddress,
			@QueryParameter String projectKey) {

		ListBoxModel listBoxModel = new ListBoxModel();

        if (StringUtils.isBlank(serverAddress)) {
            ListBoxModel mi = fetchServerList(serverAddress);
            if(mi.size() > 0) {
                serverAddress = mi.get(0).value;
            }
        }

        if(StringUtils.isBlank(projectKey)) {
            ListBoxModel mi = fetchProjectList(serverAddress);
            if(mi.size() > 0) {
                projectKey = mi.get(0).value;
            }
        }

        try {
            if(!StringUtils.isBlank(projectKey)) {
                Long projectId = Long.parseLong(projectKey);
                Long projectDuration = projectService.getProjectDurationInDays(projectId);

                if (projectDuration == -1) {
                    listBoxModel.add(CYCLE_DURATION_30_DAYS);
                    listBoxModel.add(CYCLE_DURATION_7_DAYS);
                    listBoxModel.add(CYCLE_DURATION_1_DAY);
                    return listBoxModel;
                }

                if (projectDuration >= 29) {
                    listBoxModel.add(CYCLE_DURATION_30_DAYS);
                }

                if (projectDuration >= 6) {
                    listBoxModel.add(CYCLE_DURATION_7_DAYS);
                }
            }
        }
        catch (Exception e) {
            //todo: handle exception gracefully
            e.printStackTrace();
        }

		listBoxModel.add(CYCLE_DURATION_1_DAY);
		return listBoxModel;
	}
}