package com.flinko.plugins;

import hudson.model.Action;
public class FlinkoBuildAction implements Action {

    private String message;
    private boolean pass;

    public FlinkoBuildAction() { }

    public FlinkoBuildAction(String message, boolean pass) {
        this.message = message;
        this.pass = pass;
    }

    /**
     *
     * @return the path to the icon file to be used by Jenkins. If null, no link will be generated
     */
    @Override
    public String getIconFileName() {
        return "/plugin/flinko/images/68x68/flinko.png";
    }

    @Override
    public String getDisplayName() {
        return "Flinko Build Result";
    }

    @Override
    public String getUrlName() {
        return "flinko-build-result";
    }

    /**
     * @return the correct
     */
    public boolean isPass() {
        return pass;
    }

    /**
     * @param pass the correct to set
     */
    public void setPass(boolean pass) {
        this.pass = pass;
    }

    @Override
    public String toString() {
        return String.format("%s", message);
    }

}
