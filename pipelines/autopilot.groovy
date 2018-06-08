import com.cloudbees.hudson.plugins.folder.Folder
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.extensions.GitSCMExtension;
import hudson.plugins.git.extensions.impl.CheckoutOption;
import hudson.plugins.git.extensions.impl.CloneOption;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.SubmoduleConfig;
import hudson.plugins.git.UserRemoteConfig;
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun

def defaultFolderName = 'Lift'
def defaultViewName = 'AutoPilot'

//def jobs = Hudson.instance.getAllItems(WorkflowJob)*.fullName
//echo "${jobs}"

Folder liftFolder = Jenkins.getInstance().getItem(defaultFolderName)
if(liftFolder == null) {
   liftFolder = Jenkins.getInstance().createProject(Folder.class, defaultFolderName);
}

autoPilotView = liftFolder.getView(defaultViewName)
if(autoPilotView == null) {
    liftFolder.addView(new ListView(defaultViewName))
}

def now = new Date()
def currentHour = now.format("HH", TimeZone.getDefault())
echo "current hour is ${currentHour}"

node() {
    def currentPath = "${env.WORKSPACE}\\pipelines\\autopilot_params\\${currentHour}\\"
	echo "${currentPath}"
	
	checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: 'test/dynamic_pipeline_in_Lift']], 
             doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], 
             userRemoteConfigs: [[credentialsId: "b6daa83e-1669-4908-baee-554f27a49a40", 
             refspec: '', url: 'git@github.com:logikeer/Testing.git']]]

    def fileList=[]
    (currentPath as File).eachFile groovy.io.FileType.DIRECTORIES, {
        fileList << it
		echo "${it}"
    }
	echo "${fileList}"
}

/*
Folder customFolder = Jenkins.getInstance().getItem("customFolder")
if(customFolder == null) {
    echo "folder is null, creating folder..."
    customFolder = Jenkins.getInstance().createProject(Folder.class, "customFolder");
    customView.add(customFolder)
}

WorkflowJob customPipeline = customFolder.getItem("customPipeline")
if(customPipeline == null) {
    echo "pipeline is null, creating pipeline..."
    customPipeline = customFolder.createProject(WorkflowJob.class, "customPipeline");
}
*/
/*
ParameterDefinition paramDef = new StringParameterDefinition("VM_CREATOR", "vmware");
customPipeline.addProperty(new ParametersDefinitionProperty(paramDef));

CloneOption cloneOption = new CloneOption(false, true, null, 60);
cloneOption.setDepth(0);
CheckoutOption checkoutOption = new CheckoutOption(60);
def gitScmExtensionList = [cloneOption, checkoutOption];

CpsScmFlowDefinition cpsScmFlowDefinition = new CpsScmFlowDefinition(
    new GitSCM(
        Collections.singletonList(new UserRemoteConfig("git@10.45.22.48:deep-security/lift-project.git", null, null, "b6daa83e-1669-4908-baee-554f27a49a40")),
        Collections.singletonList(new BranchSpec("master")),
        false, 
        Collections.<SubmoduleConfig>emptyList(),
        null,
        null,
        gitScmExtensionList
    ),
    "pipelines/lift-docker.groovy");

customPipeline.setDefinition(cpsScmFlowDefinition);
customPipeline.buildDiscarder = new hudson.tasks.LogRotator(10,20,-1,-1)

WorkflowRun workflowRun = customPipeline.scheduleBuild2(0).waitForStart();
*/