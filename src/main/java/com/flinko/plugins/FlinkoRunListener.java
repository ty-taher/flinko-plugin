package com.flinko.plugins;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.listeners.RunListener;

import java.util.Collection;

/**
 *
 * @author Mads
 */
@Extension
public class FlinkoRunListener extends RunListener<AbstractBuild<?,?>> {

    int countPass = 0;
    int countFail = 0;

    @Override
    public void onCompleted(AbstractBuild<?,?> r, TaskListener tl) {

        Collection<FlinkoBuildAction> actions = r.getActions(FlinkoBuildAction.class);
        for (FlinkoBuildAction action : actions) {
            if(action.isPass()) {
                countPass++;
            } else {
                countFail++;
            }
        }
        super.onCompleted(r, tl);
    }

}
