package com.flinko.plugins;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.ProminentProjectAction;

import java.util.Collection;

public class FlinkoProjectAction implements ProminentProjectAction {

    public final AbstractProject<?,?> project;

    /**
     * If this method returns null, no icon will be used and the link will not be visible
     * @return the path to the icon we want to use for our project action.
     */
    @Override
    public String getIconFileName() {
        return "/plugin/flinko/images/68x68/flinko.png";
    }

    /**
     * This method is used to create the text for the link to 'Project Action' link on the Job's
     * front page.
     *
     * @return the text to be displayed on the project action link page
     */
    @Override
    public String getDisplayName() {
        return "Flinko statistics";
    }

    @Override
    public String getUrlName() {
        return "flinko-statistics";
    }

    public FlinkoProjectAction(AbstractProject<?,?> project) {
        this.project = project;
    }

    /**
     *
     * @return the last build action associated with this project.
     */
    public Collection<FlinkoBuildAction> getLastBuildActions() {
        AbstractBuild<?, ?> b = project.getLastCompletedBuild();
        return b == null ? null : b.getActions(FlinkoBuildAction.class);
    }

    public int getLocalPass() {
        return getLocalPassFail()[0];
    }


    public int getLocalFail() {
        return getLocalPassFail()[1];
    }

    private int[] getLocalPassFail() {
        int passCnt = 0, failCnt = 0;
        for( AbstractBuild<?, ?> b = project.getLastCompletedBuild() ; b != null ; b = b.getPreviousBuild() ) {
            Collection<FlinkoBuildAction> actions = b.getActions( FlinkoBuildAction.class );
            for (FlinkoBuildAction action : actions) {
                if (action.isPass()) {
                    passCnt++;
                } else {
                    failCnt++;
                }
            }
        }
        return new int[] { passCnt, failCnt };
    }
}
