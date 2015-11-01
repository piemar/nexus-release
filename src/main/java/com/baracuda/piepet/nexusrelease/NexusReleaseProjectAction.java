package com.baracuda.piepet.nexusrelease;

import hudson.model.*;
import org.apache.commons.fileupload.FileItem;
import com.baracuda.piepet.nexusrelease.nexus.Stage;
import com.baracuda.piepet.nexusrelease.nexus.StageClient;
import com.baracuda.piepet.nexusrelease.nexus.StageException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by piepet on.
 */
public class NexusReleaseProjectAction implements Action {

    private AbstractProject<?, ?> project;

    @Override
    public String getIconFileName() {
        return "/plugin/nexus-release/img/nexus-logo.png";
    }

    @Override
    public String getDisplayName() {
        return "Perform Nexus Release";
    }

    @Override
    public String getUrlName() {
        return "nexus-releasePA2";
    }

    public AbstractProject<?, ?> getProject() {
        return this.project;
    }

    public String getProjectName() {
        return this.project.getName();
    }
    private Stage lookupByStageId(String stageID, List<Stage> stages){
        Stage returnStage=null;
        for (Stage stage : stages) {
            if(stage.getStageID().equals(stageID)){
                returnStage = stage;
            }
        }
        return returnStage;
    }
    public void doUpdateStageState(StaplerRequest req, StaplerResponse resp) throws IOException, ServletException {
        StaplerRequestWrapper requestWrapper = new StaplerRequestWrapper(req);
        NexusReleasePublisher.DescriptorImpl descriptor = (NexusReleasePublisher.DescriptorImpl) project.getLastBuild().getDescriptorByName("NexusReleasePublisher");
        String username=requestWrapper.getString("nexusUsername");
        String userPassword=requestWrapper.getString("nexusPassword");

        URL url= null;
        try {
            url = new URL(descriptor.getNexusUrl());
            StageClient stageClient=new StageClient(url, username, userPassword);
            stageClient.checkAuthentication();
            String [] releases=req.getParameterValues("releaseStageID");
            String [] drops=req.getParameterValues("dropStageID");
            List<Stage> stages = getClosedStagingRepositories();
            if(stages!=null) {
                if(releases!=null) {
                    for (String release : releases) {
                        Stage stageToRelease = lookupByStageId(release, stages);
                        stageClient.releaseStage(stageToRelease);
                        if(descriptor.getAutoDropAfterRelease()){
                            stageClient.dropStage(stageToRelease);
                        }
                    }
                }
                if (drops != null) {

                    for (String drop : drops) {
                        Stage stageToDrop = lookupByStageId(drop, stages);
                        stageClient.dropStage(stageToDrop);
                    }
                }
            }


        } catch (MalformedURLException e) {
            LOGGER.log(Level.INFO,"URL to Nexus server is not correct, please revisit configuration");
        } catch (StageException e) {
            LOGGER.log(Level.INFO, e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + '/' + project.getUrl());
    }

    /**
     * Wrapper to access request data with a special treatment if POST is multipart encoded
     */
    static class StaplerRequestWrapper {
        private final StaplerRequest request;
        private Map<String, FileItem> parsedFormData;
        private boolean isMultipartEncoded;

        public StaplerRequestWrapper(StaplerRequest request) throws ServletException {
            this.request = request;

            // JENKINS-16043, POST can be multipart encoded if there's a file parameter in the job
            String ct = request.getContentType();
            if (ct != null && ct.startsWith("multipart/")) {
                // as multipart content can only be read once, we can't read it here, otherwise it would
                // break request.getSubmittedForm(). So, we get it using reflection by reading private
                // field parsedFormData

                // ensure parsedFormData field is filled
                request.getSubmittedForm();

                try {
                    java.lang.reflect.Field privateField = org.kohsuke.stapler.RequestImpl.class.getDeclaredField("parsedFormData");
                    privateField.setAccessible(true);
                    parsedFormData = (Map<String, FileItem>) privateField.get(request);
                } catch (NoSuchFieldException e) {
                    throw new IllegalArgumentException(e);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException(e);
                }

                isMultipartEncoded = true;
            } else {
                isMultipartEncoded = false;
            }
        }

        /**
         * returns the value of the key as a String. if multiple values have been
         * submitted, the first one will be returned.
         *
         * @param key
         * @return
         */
        private String getString(String key) throws javax.servlet.ServletException, java.io.IOException {
            if (isMultipartEncoded) {
                // borrowed from org.kohsuke.staple.RequestImpl
                FileItem item = parsedFormData.get(key);
                if (item!=null && item.isFormField()) {
                    if (item.getContentType() == null && request.getCharacterEncoding() != null) {
                        // JENKINS-11543: If client doesn't set charset per part, use request encoding
                        try {
                            return item.getString(request.getCharacterEncoding());
                        } catch (java.io.UnsupportedEncodingException uee) {
                            LOGGER.log(Level.WARNING, "Request has unsupported charset, using default for '"+key+"' parameter", uee);
                            return item.getString();
                        }
                    } else {
                        return item.getString();
                    }
                } else {
                    throw new IllegalArgumentException("Parameter not found: " + key);
                }
            } else {
                return (String) (((Object[]) request.getParameterMap().get(key))[0]);
            }
        }

        /**
         * returns true if request contains key
         *
         * @param key parameter name
         * @return
         */
        private boolean containsKey(String key) throws javax.servlet.ServletException, java.io.IOException {
            // JENKINS-16043, POST can be multipart encoded if there's a file parameter in the job
            if (isMultipartEncoded) {
                return parsedFormData.containsKey(key);
            } else {
                return request.getParameterMap().containsKey(key);
            }
        }
    }
    private String lookupProfileIdFromBuildParameters(List<ParametersAction> parametersActions){

        String returnValue="";
        for (ParametersAction parametersAction : parametersActions) {
            List<ParameterValue> parameters=parametersAction.getParameters();
            for (ParameterValue parameter : parameters) {
                String name=parameter.getName();
                if(name.equals("NEXUS_STAGING_PROFILEID")){
                    returnValue=parameter.getValue().toString();
                }
            }
        }
        return returnValue;
    }
    public List<Stage> getClosedStagingRepositories() {
        NexusReleasePublisher.DescriptorImpl descriptor = (NexusReleasePublisher.DescriptorImpl) project.getLastBuild().getDescriptorByName("NexusReleasePublisher");
        List<ParametersAction> parametersActions =project.getLastBuild().getActions(ParametersAction.class);
        List<Stage> stages=null;
        URL url= null;
        try {
            url = new URL(descriptor.getNexusUrl());
            StageClient stageClient=new StageClient(url, descriptor.getNexusUsername(), descriptor.getNexusPassword());
            stageClient.checkAuthentication();
            stages=stageClient.getStagingRepositories(lookupProfileIdFromBuildParameters(parametersActions));

        } catch (MalformedURLException e) {
            LOGGER.log(Level.INFO,"URL to Nexus server is not correct, please revisit configuration");
        } catch (StageException e) {
            LOGGER.log(Level.INFO, e.getMessage());
        }
        return stages;
        //stageClient.closeStage(stages.get(0),"Closed by Nexus Release Plugin");
    }

    NexusReleaseProjectAction(final AbstractProject<?, ?> project) {
        this.project = project;
    }
    private static final Logger LOGGER = Logger.getLogger(NexusReleaseProjectAction.class.getName());

}
