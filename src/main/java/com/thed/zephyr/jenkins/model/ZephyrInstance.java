package com.thed.zephyr.jenkins.model;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.thed.service.UserService;
import com.thed.service.impl.UserServiceImpl;
import com.thed.zephyr.jenkins.utils.URLValidator;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpResponseException;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import java.util.Collections;
import java.util.logging.Logger;

public class ZephyrInstance extends AbstractDescribableImpl<ZephyrInstance> {

	private String serverAddress;
    private String credentialsId;

    @DataBoundConstructor
    public ZephyrInstance(String serverAddress, String credentialsId) {
        setServerAddress(serverAddress);
        setCredentialsId(credentialsId);
    }

    public ZephyrInstance() {}


	
	public String getServerAddress() {
		return serverAddress;
	}

	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ZephyrInstance> {

        private static Logger logger = Logger.getLogger(ZephyrInstance.class.getName());

        private UserService userService = new UserServiceImpl();

        public String getDisplayName() { return ""; }

        public ListBoxModel doFillCredentialsIdItems(@QueryParameter String credentialsId) {
            StandardListBoxModel result = new StandardListBoxModel();
            if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
                return result.includeCurrentValue(credentialsId);
            }
            return result
                    .includeAs(ACL.SYSTEM, Jenkins.getInstance(), StringCredentials.class)
                    .includeAs(ACL.SYSTEM, Jenkins.getInstance(), StandardUsernamePasswordCredentials.class)
                    .includeCurrentValue(credentialsId); // (5)
        }

        private StandardCredentials getCredentialsFromId(String credentialsId) {
            Iterable<StandardCredentials> credentials = CredentialsProvider.lookupCredentials(StandardCredentials.class,
                    Jenkins.getInstance(),
                    ACL.SYSTEM,
                    Collections.<DomainRequirement>emptyList());

            return CredentialsMatchers.firstOrNull(
                    credentials,
                    CredentialsMatchers.withId(credentialsId));
        }

        @POST
        public FormValidation doTestConnection(
                @QueryParameter String serverAddress, @QueryParameter String credentialsId) {

            Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);

            if (StringUtils.isBlank(serverAddress)) {
                return FormValidation.error("Please enter the server url.");
            }

            if(StringUtils.isBlank(credentialsId)) {
                return FormValidation.error("Please select credentials.");
            }

            StandardCredentials upCredentials = getCredentialsFromId(credentialsId);

            if(upCredentials == null) {
                return FormValidation.error("Please select valid credentials.");
            }

            if (!(serverAddress.trim().startsWith("https://") || serverAddress
                    .trim().startsWith("http://"))) {
                return FormValidation.error("Incorrect server address format");
            }

            String zephyrURL = URLValidator.validateURL(serverAddress);

            try {
                if(upCredentials instanceof UsernamePasswordCredentialsImpl) {
                    Boolean verifyCredentials = userService.verifyCredentials(serverAddress, ((UsernamePasswordCredentialsImpl) upCredentials).getUsername(),
                            ((UsernamePasswordCredentialsImpl) upCredentials).getPassword().getPlainText());
                    if (!verifyCredentials) {
                        return FormValidation.error("Username or password is incorrect.");
                    }
                }else if(upCredentials instanceof StringCredentialsImpl){
                    Boolean verifyCredentials = userService.verifyCredentials(serverAddress, ((StringCredentialsImpl) upCredentials).getSecret().getPlainText());
                    if (!verifyCredentials) {
                        return FormValidation.error("Api token is incorrect.");
                    }
                }
            }
            catch (HttpResponseException e) {
                return FormValidation.error("Error occurred while verifying credentials. Please try again later.\n" + e.getMessage());
            }
            catch (Exception e) {
                return FormValidation.error("Error occurred while verifying credentials. Please try again later.\n" + e.toString());
            }

            return FormValidation.ok("Connection to Zephyr has been validated");
        }

    }

}
