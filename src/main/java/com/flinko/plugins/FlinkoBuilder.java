package com.flinko.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FlinkoBuilder  extends Builder implements SimpleBuildStep, Describable<Builder> {

    private  String url;
    private  String userName;
    private  String password;
    private  String project;
    private  String suite;

    static Cookie pjtCookie = new Cookie("projectId", null);
    static Cookie sutCookie = new Cookie("suiteId", null);


    public static final String SIGN_IN_URL = ":8101/optimize/v1/public/user/signin";
    public static final String PROJECTS_URL = ":8102/optimize/v1/projects/all";
    public static final String SUITES_URL = ":8102/optimize/v1/suite/getAll/all";
    public static final String SUITE_EXECUTION_URL = ":8109/optimize/v1/dashboard/execution/suite/";
    public static final String EXECUTION_DETAIL = ":8109/optimize/v1/dashboard/execution/";

    private static Logger logger = Logger.getLogger(FlinkoBuilder.class.getName());

    @DataBoundConstructor
    public FlinkoBuilder(String url, String userName, String password, String project, String suite) {
        this.url = url;
        this.userName = userName;
        this.password = password;
        this.project = project;
        this.suite = suite;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getSuite() {
        return suite;
    }

    public void setSuite(String suite) {
        this.suite = suite;
    }

    public void saveFlinkoStats(AbstractBuild<?, ?> build, String message, boolean result) throws IOException {
        build.addAction(new FlinkoBuildAction(message, result));
        // Add a FlinkoRecorder if not already done
        AbstractProject<?,?> project = build.getProject();
        if(project.getPublishersList().getAll(FlinkoRecorder.class).isEmpty()) {
            project.getPublishersList().add(new FlinkoRecorder());
            project.save();
        }

    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException {

        PrintStream log = listener.getLogger();

        log.println("==================== Build started ====================");
        log.println("Parameters - url.:" + url + ", user name.:" + userName + ", password.:" + password );
        String projectId = pjtCookie.getValue();
        String suiteId = sutCookie.getValue();
        log.println("project name.:" + project + ", project id.:" + projectId + ", suite name.:" + suite + ", suite id.:" + suiteId);

        log.println("==================== Validation started ====================");
        boolean result = true;
        String message = "";
        if (Util.fixEmptyAndTrim(url) == null) {
            message = "ERROR.: URL is missing!!!";
            log.println();
            result =  false;
        } else if(Util.fixEmptyAndTrim(userName) == null) {
            message = "ERROR.: USER_NAME is missing!!!";
            result =  false;
        } else if(Util.fixEmptyAndTrim(password) == null) {
            message = "ERROR.: PASSWORD is missing!!!";
            result =  false;
        } else if(Util.fixEmptyAndTrim(projectId) == null) {
            message = "ERROR.: PROJECT is missing!!!";
            result =  false;
        } else if(Util.fixEmptyAndTrim(suiteId) == null) {
            message = "ERROR.: SUITE is missing!!!";
            result =  false;
        }

        if (!result) {
            log.println(message);
            saveFlinkoStats(build, message, result);
            return result;
        }

        JSONObject httpResponse = null;
        Map<String, String> body = new HashMap<>();
        body.put("emailId", userName);
        body.put("password", password);
        log.println("Request Body.:" + body);

        try {
            httpResponse = validateUser(url+SIGN_IN_URL, body);
            if (httpResponse.getInt("responseCode") != HttpStatus.SC_OK) {
                message = "XXXXXXXXXXX ERROR XXXXXXXXXXX - User Validation Failed.: " + httpResponse.getString("message");
                log.println(message);
                result = false;
                saveFlinkoStats(build, message, result);
                return result;
            } else {
                log.println("==================== Suite execution started ====================");
                JSONObject userResponseJson = httpResponse.getJSONObject("responseObject");
                String token = userResponseJson.getString("access_token");
                String suiteExecutionUrl = url + SUITE_EXECUTION_URL + suiteId;
                log.println("Suite execution creation url.: " + suiteExecutionUrl);
                String executionResponse = FlinkoRestApi.executeSuite(suiteExecutionUrl, token);
                JSONObject executionResponseJson = new JSONObject(executionResponse);
                if (executionResponseJson.getInt("responseCode") != HttpStatus.SC_CREATED) {
                    message = "XXXXXXXXXXX ERROR XXXXXXXXXXX: Suite Execution Failed.: " + executionResponseJson.getString("message");
                    log.println(message);
                    result = false;
                    saveFlinkoStats(build, message, result);
                    return result;
                } else {
                    JSONObject responseObject = executionResponseJson.getJSONObject("responseObject");
                    int MILLI_SECOND = 45000;
                    String executionDetailsUrl =  url + EXECUTION_DETAIL + responseObject.getString("id");
                    log.println("Suite execution instance url.: " + executionDetailsUrl);
                    while (isSuiteExecuting(responseObject.getString("executionStatus"))) {
                        /*
                        if (checkForLongTime(responseObject.getString("createdOn"))) {
                            log.println("XXXXXXXXXXX TERMINATING JOB: EXECUTION TAKING LONG TIME!!! XXXXXXXXXXX");
                            return false;
                        }
                        */
                        executionResponse = FlinkoRestApi.getSuiteExecutionDetails(executionDetailsUrl, token);
                        executionResponseJson = new JSONObject(executionResponse);
                        if (executionResponseJson.getInt("responseCode") == HttpStatus.SC_OK) {
                            responseObject = executionResponseJson.getJSONObject("responseObject");
                        } else {
                            message = "XXXXXXXXXXX ERROR XXXXXXXXXXX.:" + executionResponse;
                            log.println(message);
                            result = false;
                            saveFlinkoStats(build, message, result);
                            return result;
                        }
                        log.println("Execution Status.: " + responseObject.getString("executionStatus"));
                        if(isSuiteExecuting(responseObject.getString("executionStatus"))){
                            log.println("Executing ...");
                            Thread.sleep(MILLI_SECOND);
                        }
                    }
                    String executionStatus = responseObject.has("executionStatus") ? responseObject.getString("executionStatus"): "";
                    String resultStatus = responseObject.has("resultStatus") ? responseObject.getString("resultStatus"): "";
                    message = "Execution Status.: " + executionStatus + ", Result Status.:" + resultStatus;
                    log.println("Final Result - " + message);
                    if (isSuiteExecutionPass(executionStatus, resultStatus)) {
                        log.println("------------------------------------ EXECUTION PASSED ------------------------------------");
                        result = true;
                        saveFlinkoStats(build, message, result);
                        return result;
                    } else {
                        log.println("------------------------------------ EXECUTION FAILED ------------------------------------");
                        result = false;
                        saveFlinkoStats(build, message, result);
                        return result;
                    }
                }
            }
        } catch (JSONException e) {
            message = "XXXXXXXXXXX ERROR XXXXXXXXXXX.: " + e.getLocalizedMessage();
            log.println(message);
            e.printStackTrace();
            result = false;
            saveFlinkoStats(build, message, result);
            return result;
        } catch (Exception e) {
            message = "XXXXXXXXXXX ERROR XXXXXXXXXXX.: " + e.getLocalizedMessage();
            log.println(message);
            e.printStackTrace();
            result = false;
            saveFlinkoStats(build, message, result);
            return result;
        }
    }

    public static boolean checkForLongTime(String dateTime) throws ParseException {

        //system time
        Date time = new Date();

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        Date dt = sdf.parse(dateTime);

        System.out.println("system, time: " + sdf.format(time) );
        System.out.println("given dateTime: " +dateTime );

        long diffInMillies = Math.abs(time.getTime() - dt.getTime());
        long diffInHrs = TimeUnit.HOURS.convert(diffInMillies, TimeUnit.MILLISECONDS);

        System.out.println("difference in hours" + diffInHrs  + ", diff In Millies" + diffInMillies);
        long THRESHOLD_HRS = 12;
        if (diffInHrs > THRESHOLD_HRS) {
            return true;
        }
        return false;
    }


    private boolean isSuiteExecutionPass(String executionStatus, String resultStatus) {
        return (executionStatus.equalsIgnoreCase("Completed") && (resultStatus.equalsIgnoreCase("PASS") || resultStatus.equalsIgnoreCase("WARNING") || resultStatus.equalsIgnoreCase("SKIP")));
    }

    private boolean isSuiteExecuting(String executionStatus) {
        return executionStatus.equalsIgnoreCase("Running") || executionStatus.equalsIgnoreCase("Pending");
    }

    private static JSONObject validateUser(String url, Map<String, String> body) throws IOException, JSONException {
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody = objectMapper
                .writeValueAsString(body);
        String result = FlinkoRestApi.postRequest(url, requestBody);
        return new JSONObject(result);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder>{

        public List<HashMap<String, String>> projects  = new ArrayList<>();
        public List<HashMap<String, String>> suites  = new ArrayList<>();
        public String baseServerUrl = null;
        public String token = null;

        @JavaScriptMethod
        public static String getProjectId() {
            return FlinkoBuilder.pjtCookie.getValue();
        }

        @JavaScriptMethod
        public static void setProjectId(String projectId) {
            if(FlinkoBuilder.pjtCookie.getValue() == null) {
                FlinkoBuilder.pjtCookie.setMaxAge(60 * 60 * 24 * 365 * 10);//10YRS
            }
            FlinkoBuilder.pjtCookie.setValue(projectId);
        }

        @JavaScriptMethod
        public static String getSuiteId() {
            return FlinkoBuilder.sutCookie.getValue();
        }

        @JavaScriptMethod
        public static void setSuiteId(String projectId) {
            if(FlinkoBuilder.sutCookie.getValue() == null) {
                FlinkoBuilder.sutCookie.setMaxAge(60 * 60 * 24 * 365 * 10);//10YRS
            }
            FlinkoBuilder.sutCookie.setValue(projectId);
        }

//        EnvVars envVars = new EnvVars();
        private int lastEditorId = 0;

        public DescriptorImpl() {
            load();
        }

        @JavaScriptMethod
        public synchronized String createEditorId() {
            return String.valueOf(lastEditorId++);
        }

        @JavaScriptMethod
        public List<HashMap<String, String>> fetchProjects() {
            System.out.println("I nside fetch projects!!");
            System.out.println("Before UI:" + projects);
            return projects;
        }

        @JavaScriptMethod
        public List<HashMap<String, String>> fetchSuites() {
            System.out.println("Inside fetchoffers!!");
            System.out.println("Before UI:" + suites);
            return suites;
        }

        @JavaScriptMethod
        public List<HashMap<String, String>> getSuites(
                final String userName,
                final String password,
                final String projectId) throws JSONException, IOException {
                suites = new ArrayList<>();
                Map<String, String> body = new HashMap<>();
                body.put("emailId", userName);
                body.put("password", password);
                JSONObject httpAuthResponse = validateUser(baseServerUrl + SIGN_IN_URL, body);
                if (httpAuthResponse.getInt("responseCode") != HttpStatus.SC_OK) {
                    token = null;
                    return suites;
                } else {
                    JSONObject authResponseBody = httpAuthResponse.getJSONObject("responseObject");
                    token = authResponseBody.getString("access_token");
                    String projectsRes = FlinkoRestApi.getSuites(baseServerUrl + SUITES_URL, token, projectId);
                    JSONObject httpProjectResponse = new JSONObject(projectsRes);
                    if (httpProjectResponse.getInt("responseCode") == HttpStatus.SC_OK) {
                        JSONArray jsonArray = httpProjectResponse.getJSONArray("responseObject");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject object = (JSONObject) jsonArray.get(i);
                            HashMap<String , String> map = new HashMap<>();
                            map.put("id", object.getString("id"));
                            map.put("name", object.getString("name"));
                            suites.add(map);
                        }
                    }
                    return suites;
                }
        }

        public static boolean isEmpty(String str) {
            return str == null || "".equals(str);
        }
        public FormValidation doTestConnection(@QueryParameter("url") final String url,
                                               @QueryParameter("userName") final String userName,
                                               @QueryParameter("password") final String password) throws JSONException, IOException {
                Map<String, String> body = new HashMap<>();
                body.put("emailId", userName);
                body.put("password", password);
                baseServerUrl = url;
                JSONObject httpAuthResponse = validateUser(baseServerUrl + SIGN_IN_URL, body);
                if (httpAuthResponse.getInt("responseCode") != HttpStatus.SC_OK) {
                    token = null;
                    return FormValidation.error("Validation Failed!: " + httpAuthResponse.getString("message"));
                }
                else {
                    JSONObject authResponseBody = httpAuthResponse.getJSONObject("responseObject");
                    token = authResponseBody.getString("access_token");
                    String projectsRes = FlinkoRestApi.getRequest(baseServerUrl + PROJECTS_URL, token);
                    JSONObject httpProjectResponse = new JSONObject(projectsRes);
                    if (httpProjectResponse.getInt("responseCode") == HttpStatus.SC_OK) {
                        JSONArray jsonArray = httpProjectResponse.getJSONArray("responseObject");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject object = (JSONObject) jsonArray.get(i);
                            HashMap<String , String> map = new HashMap<>();
                            map.put("id", object.getString("id"));
                            map.put("name", object.getString("name"));
                            projects.add(map);
                        }
                    }
                    return FormValidation.ok("Success");
                }
        }



        public FormValidation doCheckUrl(@QueryParameter String value) {
            if (Util.fixEmptyAndTrim(value) == null) {
                return FormValidation.error(Messages.FlinkoBuilder_DescriptorImpl_errors_missingUrl());
            }
            baseServerUrl = value;
            return FormValidation.ok();
        }
        public FormValidation doCheckUserName(@QueryParameter String value) {
            if (Util.fixEmptyAndTrim(value) == null) {
                return FormValidation.error(Messages.FlinkoBuilder_DescriptorImpl_errors_missingUsername());
            }
            return FormValidation.ok();
        }
        public FormValidation doCheckPassword(@QueryParameter String value) {
            if (Util.fixEmptyAndTrim(value) == null) {
                return FormValidation.error(Messages.FlinkoBuilder_DescriptorImpl_errors_missingPassword());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckProject(@QueryParameter String value) {
            if (Util.fixEmptyAndTrim(value) == null || value.equalsIgnoreCase("-none-")) {
                return FormValidation.error(Messages.FlinkoBuilder_DescriptorImpl_errors_missingProject());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckSuite(@QueryParameter String value) {
            if (Util.fixEmptyAndTrim(value) == null || value.equalsIgnoreCase("-none-")) {
                return FormValidation.error(Messages.FlinkoBuilder_DescriptorImpl_errors_missingSuite());
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillSuiteItems(@QueryParameter("suite") String suite) throws IOException, JSONException {
            System.out.println("inside do fill suite:" + suite);
            return new ListBoxModel(new ListBoxModel.Option(suite));
        }

        public ListBoxModel doFillProjectItems(@QueryParameter("project") String project) throws JSONException, IOException {
            Logger.getLogger(FlinkoBuilder.class.getName()).log(Level.INFO, "inside do fill project:" + project);
            System.out.println("inside do fill project:" + project);
            ListBoxModel listBoxModel = new ListBoxModel(new ListBoxModel.Option(project));
            Logger.getLogger(FlinkoBuilder.class.getName()).log(Level.INFO, "inside do fill project:" + listBoxModel.toString());
            System.out.println(listBoxModel);
            return listBoxModel;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.FlinkoBuilder_DescriptorImpl_DisplayName();
        }

        @Override
        public boolean configure(StaplerRequest req, net.sf.json.JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            req.bindJSON(this, formData);
            save();
            return super.configure(req, formData);
        }

    }
}
