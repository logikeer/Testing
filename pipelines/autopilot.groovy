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
        jobList << it.getCanonicalPath()
    }
	echo "Job list: ${jobList}"
	
	return jobList
}

@NonCPS
def updateJobInJenkins(jobList, defaultFolderName, defaultViewName, currentPath) {
	echo "update job(s)...."
	
	Folder liftFolder = Jenkins.getInstance().getItem(defaultFolderName)
	if(liftFolder == null) {
		liftFolder = Jenkins.getInstance().createProject(Folder.class, defaultFolderName);
	}
	
	def autoPilotView = liftFolder.getView(defaultViewName)
	if(autoPilotView == null) {
		autoPilotView = liftFolder.addView(new ListView(defaultViewName))
	}
	
	for(i=0; i<jobList.size(); i++) {
		echo "process job: ${jobList[i]}"
		
		String extendPath = jobList[i] - (currentPath + '\\')
		echo "extendPath: ${extendPath}"
		
		String[] componentList = extendPath.split('\\\\')
		echo "componentList: ${componentList}"

		// create folder
		Folder folder = liftFolder
		for(j=0; j<componentList.size()-1; j++) {
			// if this is the 1st folder, add this folder in the view
			Folder folderInstance = folder.getItem(componentList[i]);
			if(folderInstance == null) {
				echo "folder is null, creating folder ${componentList[i]}"
				folder = folder.createProject(Folder.class, componentList[i])
				
				// add the 1st folder in the view
				if(j == 0) {
					echo "add folder ${componentList[i]} in view"
					autoPilotView.add(folder)
				}
			} else {
				echo "go to folder ${componentList[i]}"
				folder = folderInstance
			}
		}
		
		// create pipeline
		String pipelineName = componentList[componentList.size()-1]
		echo "pipeline name is ${pipelineName}"
		
		WorkflowJob pipeline = folder.getItem(pipelineName)
		if(pipeline == null) {
			echo "pipeline is null, creating pipeline ${pipelineName}"
			pipeline = folder.createProject(WorkflowJob.class, pipelineName);
		} else {
			echo "go to pipeline ${pipelineName}"
		}

		// update pipeline content
		pipeline.removeProperty(ParametersDefinitionProperty.class);
		def parameterPath = jobList[i].replace("${WORKSPACE}", "")
		def parameterFile = load "${parameterPath}"
		echo "parameterFile is ${parameterFile}"
		parameterFile.getParameterMap().each { k, v ->
			echo "string parameter: ${k} = ${v}"
			
			ParameterDefinition paramDef = new StringParameterDefinition("${k}", "${v}");
			pipeline.addProperty(new ParametersDefinitionProperty(paramDef));
		}
		
		pipeline.buildDiscarder = new hudson.tasks.LogRotator(10, 20, -1, -1)
		
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
		pipeline.setDefinition(cpsScmFlowDefinition);
		
		echo "trigger pipeline ${pipelineName}"
	}
}

node() {
	def defaultFolderName = 'Lift'
	def defaultViewName = 'AutoPilot'

	pullLiftBranch('test/dynamic_pipeline_in_Lift')
    def currentHour = getCurrentHour()
	def currentPath = getCurrentPath(currentHour)
	
	def jobList = getJobList(currentPath)
	if(!jobList) {
		echo "there is no job(s) now..."
		System.exit(0)
	}
	
	updateJobInJenkins(jobList, defaultFolderName, defaultViewName, currentPath)
}

/*
WorkflowRun workflowRun = customPipeline.scheduleBuild2(0).waitForStart();
*/