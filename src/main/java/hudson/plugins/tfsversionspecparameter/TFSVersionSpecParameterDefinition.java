package hudson.plugins.tfsversionspecparameter;

import hudson.Extension;
import hudson.model.*;
import hudson.plugins.tfs.TeamFoundationServerScm;
import hudson.scm.SCM;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;


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

        public Map<String, String> getChangeSetMap() {
            if( changesetMap == null || changesetMap.isEmpty()){
                generateContents(PARAMETER_TYPE_CHANGESET);
            }
            return changesetMap;
        }
        
        public Map<String, String> getLabelMap() {
            if( labelMap == null || labelMap.isEmpty()){
                generateContents(PARAMETER_TYPE_LABEL);
            }
            return labelMap;
        }
        
        public void generateContents(String contenttype){
            AbstractProject<?,?> project = getParentProject();
            
            SCM scm = project.getScm();
            if (!(scm instanceof TeamFoundationServerScm)) {
                this.errorMessage = "notTFS";
                return;
            }
            TeamFoundationServerScm tfs = (TeamFoundationServerScm) scm;

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

}
