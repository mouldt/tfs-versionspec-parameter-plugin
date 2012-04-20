package hudson.plugins.tfsversionspecparameter.model;

import java.util.Date;

public class TFSLabel {
    private String label;
    private String comment;
    private String projectScope;
    private String owner;
    private Date date;
    
    public TFSLabel() {
        this("", "", null, "", "");
    }
    
    public TFSLabel(String label, String projectScope, Date date, String owner, String comment) {
        this.label = label;
        this.date = date;
        this.comment = comment;
        this.owner = owner;
        this.projectScope = projectScope;
    }
    
    public Date getDate() {
        return date;
    }
    
    public String getComment() {
        return comment;
    }
    
    public String getProjectScope() {
        return projectScope;
    }
    
    public String getOwner() {
        return owner;
    }
    
    public String getLabel() {
        return label;
    }
    
}
