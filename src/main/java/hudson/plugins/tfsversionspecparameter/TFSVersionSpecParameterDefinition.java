package hudson.plugins.tfsversionspecparameter;

import hudson.AbortException;
import hudson.Extension;
import hudson.console.ConsoleNote;
import hudson.model.*;
import hudson.plugins.tfs.TeamFoundationServerScm;
import hudson.plugins.tfs.TfTool;
import hudson.plugins.tfs.model.ChangeSet;
import hudson.plugins.tfs.model.Server;
import hudson.plugins.tfsversionspecparameter.commands.BriefHistoryCommand;
import hudson.plugins.tfsversionspecparameter.commands.LabelsForProjectCommand;
import hudson.plugins.tfsversionspecparameter.model.TFSLabel;
import hudson.scm.SCM;
import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import sun.misc.Launcher;


public class TFSVersionSpecParameterDefinition extends ParameterDefinition  implements Comparable<TFSVersionSpecParameterDefinition> {
    	private static final long serialVersionUID = 9157832967140868122L;

	public static final String PARAMETER_TYPE_LABEL = "PT_LABEL";
	public static final String PARAMETER_TYPE_CHANGESET = "PT_CHANGESET";

        private final UUID uuid;
        
	@Extension
	public static class DescriptorImpl extends ParameterDescriptor {
		@Override
		public String getDisplayName() {
			return "TFS VersionSpec Parameter";
		}
	}

        private String type;
        
        private String errorMessage;        
	private String defaultValue;        
        
        private Map<String, String> changesetMap;
        private Map<String, String> labelMap;

        @DataBoundConstructor
	public TFSVersionSpecParameterDefinition(String name, String type, String defaultValue, String description) {
		super(name, description);
		this.type = type;
		this.defaultValue = defaultValue;
                
                this.uuid = UUID.randomUUID();     
      	}
        

        @Override
        public ParameterValue createValue(StaplerRequest request, JSONObject jO) {
		Object value = jO.get("value");
		String strValue = "";
		if (value instanceof String) {
			strValue = (String)value;
		}
		else if (value instanceof JSONArray) {
			JSONArray jsonValues = (JSONArray)value;
			for(int i = 0; i < jsonValues.size(); i++) {
				strValue += jsonValues.getString(i);
				if (i < jsonValues.size() - 1) {
					strValue += ",";
				}
			}
		}
                
                if("".equals(strValue)) {
                    strValue = defaultValue;
                }

		TFSVersionSpecParameterValue parameterValue = new TFSVersionSpecParameterValue(jO.getString("name"), strValue);
		return parameterValue;
        }

        @Override
        public ParameterValue createValue(StaplerRequest request) {
		String value[] = request.getParameterValues(getName());
		if (value == null) {
			return getDefaultParameterValue();
		}
		return null;
        }
        
	@Override
	public ParameterValue getDefaultParameterValue() {
		String defValue = getDefaultValue();
		if (!StringUtils.isBlank(defValue)) {                    
			return new TFSVersionSpecParameterValue(getName(), defValue);
		}
		return super.getDefaultParameterValue();
	}


        @Override
        public String getType() {
            return type;
        }
        
	public void setType(String type) {
            if(type.equals(PARAMETER_TYPE_LABEL) || type.equals(PARAMETER_TYPE_CHANGESET) ) {
		this.type = type;
            } else {
                this.errorMessage = "wrongType";
            }
	}
        
	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
        
	public String getErrorMessage() {
            return errorMessage;
        }

        public Map<String, String> getChangeSetMap() throws IOException, InterruptedException {
            if( changesetMap == null || changesetMap.isEmpty()){
                changesetMap= new LinkedHashMap<String, String>();
            }
            return changesetMap;
        }
        
        public String getPopulateCollection()
        {
                errorMessage = "";
                generateContents(getType());
                return getErrorMessage();
        }
        
        public Map<String, String> getDataMap() {
            if (getType().equals(PARAMETER_TYPE_LABEL))
                return labelMap;
            else
                return changesetMap;
        }
        
        public Map<String, String> getLabelMap() throws IOException, InterruptedException {
            if( labelMap == null || labelMap.isEmpty()){
                labelMap= new LinkedHashMap<String, String>();
            }
            return labelMap;
        }
        
        public void generateContents(String contenttype) {
            AbstractProject<?,?> project = getParentProject();
            
            SCM scm = project.getScm();
            if (!(scm instanceof TeamFoundationServerScm)) {
                this.errorMessage = "TFS must be used as the SCM repository";
                return;
            }
            TeamFoundationServerScm tfs = (TeamFoundationServerScm) scm;
            String tfsExe = tfs.getDescriptor().getTfExecutable();
            
            Hudson hudson = Hudson.getInstance();
            
            ByteArrayOutputStream consoleStream = new ByteArrayOutputStream();
            TaskListener listener  = new StreamBuildListener(consoleStream);
            hudson.Launcher launcher = hudson.createLauncher(listener);
            TfTool tool = new TfTool(tfsExe, launcher, listener, project.getSomeWorkspace());
            Server server = new Server(tool, tfs.getServerUrl(project.getLastBuild()), tfs.getUserName(), tfs.getUserPassword());
            try{
                if(contenttype.equalsIgnoreCase(PARAMETER_TYPE_CHANGESET)) {
                    Map<String, String> map = getChangeSetMap();
                    List<ChangeSet> changesets = getChangeSets(server, tfs.getProjectPath());
                    Iterator<ChangeSet> iterator = changesets.iterator();
                    while(iterator.hasNext()) {
                        ChangeSet cs = iterator.next();
                        map.put(cs.getVersion(), cs.getVersion() + " - " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(cs.getDate()) + " - " + cs.getMsg());
                    }
                }
                
                if(contenttype.equalsIgnoreCase(PARAMETER_TYPE_LABEL)) {
                    Map<String, String> map = getLabelMap();
                    List<TFSLabel> changesets = getLabels(server, tfs.getProjectPath());
                    Iterator<TFSLabel> iterator = changesets.iterator();
                    while(iterator.hasNext()) {
                        TFSLabel lbl = iterator.next();
                        map.put(lbl.getLabel(), lbl.getLabel() + " - " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(lbl.getDate()) + " - " + lbl.getComment());
                    }
                }
                
            } catch (IOException ex) {
                this.errorMessage = consoleStream.toString();
            } catch (InterruptedException ex) {
                this.errorMessage = consoleStream.toString();
            } catch (ParseException ex) {
                this.errorMessage = consoleStream.toString();
            }
            finally
            {
                try {
                    consoleStream.close();
                } catch (IOException ex) {
                    this.errorMessage = ex.getMessage();
                }
            }
        }
        
        public AbstractProject<?,?> getParentProject() {
            AbstractProject<?,?> context = null;
            List<AbstractProject> jobs = Hudson.getInstance().getItems(AbstractProject.class);

            for(AbstractProject<?,?> project : jobs) {
                ParametersDefinitionProperty property = (ParametersDefinitionProperty) project.getProperty(ParametersDefinitionProperty.class);

                if(property != null) {
                    List<ParameterDefinition> parameterDefinitions = property.getParameterDefinitions();

                    if(parameterDefinitions != null) {
                        for(ParameterDefinition pd : parameterDefinitions) {

                            if(pd instanceof TFSVersionSpecParameterDefinition && 
                                ((TFSVersionSpecParameterDefinition) pd).compareTo(this) == 0) {
                                
                                context = project;
                                break;
                            }
                        }
                    }
                }
            }  
            
            return context;
        }

        @Override
        public int compareTo(TFSVersionSpecParameterDefinition pd) {
            if(pd.uuid.equals(uuid)) {
                return 0;
            }
            
            return -1;
        }
        
        protected List<ChangeSet> getChangeSets(Server server, String projectPath) throws IOException, InterruptedException, ParseException {
            BriefHistoryCommand command = new BriefHistoryCommand(server, projectPath);
            Reader reader = null;
            try {
                reader = server.execute(command.getArguments());
                return command.parse(reader);
            } finally {
                IOUtils.closeQuietly(reader);
            }
        }
        protected List<TFSLabel> getLabels(Server server, String projectPath) throws IOException, InterruptedException, ParseException {
            LabelsForProjectCommand command = new LabelsForProjectCommand(server, projectPath);
            Reader reader = null;
            try {
                reader = server.execute(command.getArguments());
                return command.parse(reader);
            } finally {
                IOUtils.closeQuietly(reader);
            }
        }
        
}
