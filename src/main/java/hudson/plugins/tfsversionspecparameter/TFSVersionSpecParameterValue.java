package hudson.plugins.tfsversionspecparameter;

import hudson.model.StringParameterValue;
import org.kohsuke.stapler.DataBoundConstructor;

public class TFSVersionSpecParameterValue extends StringParameterValue {
        private static final long serialVersionUID = -8244244942726975701L;
	
	@DataBoundConstructor
	public TFSVersionSpecParameterValue(String name, String value) {
		super(name, value);
	}
        

}
