package com.baracuda.piepet.nexusrelease;

import hudson.Launcher;
import hudson.Extension;
import hudson.model.*;
import hudson.tasks.*;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import com.baracuda.piepet.nexusrelease.maven.PomInterceptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sample {@link Publisher}.
 * <p/>
 * <p/>
 * When the user configures the project and enables this publisher,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link NexusReleasePublisher} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #})
 * to remember the configuration.
 * <p/>
 * <p/>
 * When a build is performed and is complete, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked.
 *
 * @author Pierre Peterssoni
 */
public class NexusReleasePublisher extends Recorder {

    private final String nexusUsername;
    private final String nexusUrl;
    private final String nexusPassword;
    private final boolean autoDropAfterRelease;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public NexusReleasePublisher(String nexusUrl, String nexusUsername, String nexusPassword, boolean autoDropAfterRelease) {
        this.nexusUsername = nexusUsername;
        this.nexusUrl = nexusUrl;
        this.nexusPassword = nexusPassword;
        this.autoDropAfterRelease = autoDropAfterRelease;
    }

    public boolean getAutoDropAfterRelease() {
        return autoDropAfterRelease;
    }

    public String getNexusUsername() {
        return nexusUsername;
    }

    public String getNexusUrl() {
        return nexusUrl;
    }

    public String getNexusPassword() {
        return nexusPassword;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        boolean isSuccessful=false;
        try {

            NexusReleaseBuildAction buildAction = new NexusReleaseBuildAction("", build);
            build.addAction(buildAction);
            build.addAction(initNexusParameters(build));
            isSuccessful=true;
        } catch (IOException e) {
            LOGGER.log(Level.INFO, e.getMessage());
        } catch (InterruptedException e) {
            LOGGER.log(Level.INFO, e.getMessage());
        }


        return isSuccessful;
    }

    private ParametersAction initNexusParameters(AbstractBuild build) throws IOException, InterruptedException {
        List<ParameterValue> parameterValues = new ArrayList<ParameterValue>();
        PomInterceptor pomInterceptor = new PomInterceptor();
        StringParameterValue stagingProfileid = new StringParameterValue("NEXUS_STAGING_PROFILEID",  pomInterceptor.getNexusStagingProfileId(build));
        parameterValues.add(stagingProfileid);
        return new ParametersAction(parameterValues);

    }
    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return new NexusReleaseProjectAction(project);
    }

    /**
     * Descriptor for {@link NexusReleasePublisher}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     * <p/>
     * <p/>
     * See <tt>src/main/resources/org/jenkinsci/plugins/testExample/NexusReleasePublisher/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         * <p/>
         * <p/>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private String nexusUsername;

        private String nexusPassword;
        private boolean autoDropAfterRelease;
        private String nexusUrl;
        /**
         * In order to load the persisted global configuration, you have to
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the browser.
         * <p/>
         * Note that returning {@link FormValidation#error(String)} does not
         * prevent the form from being saved. It just means that a message
         * will be displayed to the user.
         */
        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a name");
            if (value.length() < 4)
                return FormValidation.warning("Isn't the name too short?");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Nexus Release";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            nexusUrl = formData.getString("nexusUrl");
            nexusUsername = formData.getString("nexusUsername");
            nexusPassword = formData.getString("nexusPassword");
            autoDropAfterRelease = formData.getBoolean("autoDropAfterRelease");

            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req, formData);
        }

        public boolean getAutoDropAfterRelease() {
            return autoDropAfterRelease;
        }

        public String getNexusUsername() {
            return nexusUsername;
        }

        public String getNexusPassword() {
            return nexusPassword;
        }

        public String getNexusUrl() {
            return nexusUrl;
        }
    }
    private static final Logger LOGGER = Logger.getLogger(NexusReleasePublisher.class.getName());

}

