package hudson.plugins.tfsversionspecparameter.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for the detailed labels output from the TF tool.
 * <p>
 * An example of how the data looks.
 * <pre>
 *
 * Label  : Some Label
 * Scope  : $/ProjectPath
 * Owner  : owner
 * Date   : 21 October 2009 07:23:55
 * Comment: Some comment
 *
 * Changeset Item
 * --------- ---------------------------------------------------------------------
 * 
 * ===============================================================================
 * 
 * 
 * </pre>

 * @author Erik Ramfelt
 */
public class LabelsParser {

    private static final Pattern LABEL_PATTERN    = Pattern.compile("Label  : ");
    private static final Pattern SCOPE_PATTERN    = Pattern.compile("Scope  : ");
    private static final Pattern OWNER_PATTERN    = Pattern.compile("Owner  : ");
    private static final Pattern DATE_PATTERN     = Pattern.compile("Date   : ");
    private static final Pattern COMMENT_PATTERN  = Pattern.compile("Comment: ");
    private final BufferedReader reader;
    
    private String currentLine;
    private String label;
    private String comment;
    private String projectScope;
    private String owner;
    private Date date;
    
    public LabelsParser(Reader reader) throws IOException {
        this.reader = new BufferedReader( reader );
    }
    
    /**
     * Move to the next label
     * @return true, if there was a next label; false, if there is no next label.
     * @throws IOException
     */
    public boolean nextLabel() throws IOException {
        boolean bFound = false;
        do {
            currentLine = reader.readLine();
            if (currentLine != null)
            {
                Matcher matcher = LABEL_PATTERN.matcher(currentLine);
                if (matcher.find())
                {
                    bFound = true;                
                    label = currentLine.substring(matcher.end());

                    //Scope
                    currentLine = reader.readLine();
                    matcher = SCOPE_PATTERN.matcher(currentLine);
                    if(matcher.find()){
                        projectScope = currentLine.substring(matcher.end());
                    }

                    //Owner
                    currentLine = reader.readLine();
                    matcher = OWNER_PATTERN.matcher(currentLine);
                    if(matcher.find()){
                        owner = currentLine.substring(matcher.end());
                    }

                    //Date
                    currentLine = reader.readLine();
                    matcher = DATE_PATTERN.matcher(currentLine);
                    if(matcher.find()){
                        try {
                            date = new SimpleDateFormat("dd MMM yyyy HH:mm:ss").parse(currentLine.substring(matcher.end()));
                        } catch (ParseException ex) {
                            date = null;
                        }
                    }

                    //Comment
                    currentLine = reader.readLine();
                    matcher = COMMENT_PATTERN.matcher(currentLine);
                    if(matcher.find()){
                        comment = currentLine.substring(matcher.end());
                    }
                }
            }
        } while (currentLine != null && !bFound);
        return bFound;
    }
    
    public String getProjectScope() {
        return projectScope;
    }    
    public String getLabel() {
        return label;
    }    
    public Date getDate() {
        return date;
    }    
    public String getComment() {
        return comment;
    }    
    public String getOwner() {
        return owner;
    }    
}
