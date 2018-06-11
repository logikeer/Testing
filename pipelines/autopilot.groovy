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


//def jobs = Hudson.instance.getAllItems(WorkflowJob)*.fullName
//echo "${jobs}"

def pullLiftBranch(lift_branch) {
	echo "git is pulling ${lift_branch}..."
	
	checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/' + lift_branch]], 
	         doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], 
			 userRemoteConfigs: [[credentialsId: 'b6daa83e-1669-4908-baee-554f27a49a40', 
			 url: 'git@github.com:logikeer/Testing.git']]]
}

def getCurrentHour() {
	def now = new Date()
	def currentHour = now.format("HH", TimeZone.getDefault())
	echo "current hour is ${currentHour}"
	
	return currentHour
}

def getCurrentPath(currentHour) {
	def currentPath = "${env.WORKSPACE}\\pipelines\\autopilot_params\\${currentHour}"
	echo "current path is ${currentPath}"
	
	return currentPath
}

def getJobList(currentPath) {
    def jobList = []
    (currentPath as File).eachFileRecurse groovy.io.FileType.FILES, {
        jobList << it
    }
	echo "Job list: ${jobList}"
}

def updateJobInJenkins(jobList) {
	echo "update job(s)...."
	
	def defaultFolderName = 'Lift'
	def defaultViewName = 'AutoPilot'

	Folder liftFolder = Jenkins.getInstance().getItem(defaultFolderName)
	if(liftFolder == null) {
		liftFolder = Jenkins.getInstance().createProject(Folder.class, defaultFolderName);
	}
	
	def autoPilotView = liftFolder.getView(defaultViewName)
	if(autoPilotView == null) {
		autoPilotView = liftFolder.addView(new ListView(defaultViewName))
	}
}

def triggerJobInJenkins(jobList) {
	echo "trigger job(s)...."
}

node() {
	pullLiftBranch('test/dynamic_pipeline_in_Lift')
    def currentHour = getCurrentHour()
	def currentPath = getCurrentPath(currentHour)
	
	def jobList = getJobList(currentPath)
	updateJobInJenkins(jobList)
	triggerJobInJenkins(jobList)
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