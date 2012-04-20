package hudson.plugins.tfsversionspecparameter.commands;

import hudson.Util;
import hudson.plugins.tfs.commands.AbstractCommand;
import hudson.plugins.tfs.commands.ParseableCommand;
import hudson.plugins.tfs.commands.ServerConfigurationProvider;
import hudson.plugins.tfs.model.ChangeSet;
import hudson.plugins.tfs.util.DateUtil;
import hudson.plugins.tfs.util.TextTableParser;
import hudson.plugins.tfs.util.MaskedArgumentListBuilder;
import hudson.plugins.tfsversionspecparameter.model.TFSLabel;
import hudson.plugins.tfsversionspecparameter.util.LabelsParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TF command for retrieving a brief history.
 * 
 * @author Erik Ramfelt
 */
public class LabelsForProjectCommand extends AbstractCommand implements ParseableCommand<List<TFSLabel>> {
    
    private static final Pattern PROJECT_PATTERN = Pattern.compile("\\$/[a-zA-Z0-9 _-]+");
    private final String projectPath;
    
    /**
     * 
     * @param projectPath the project path to get the history for
     * @param fromTimestamp the timestamp to get history from
     * @param toTimestamp the timestamp to get history to
     */
    public LabelsForProjectCommand(ServerConfigurationProvider provider,
            String projectPath) {
        super(provider);
        Matcher matcher = PROJECT_PATTERN.matcher(projectPath);
        if (matcher.find())
            this.projectPath = matcher.group();
        else            
            this.projectPath = projectPath;
    }

    /**
     * Returns the arguments for the command
     * @return arguments for the command.
     */
    @Override
    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();        
        arguments.add("labels");
        arguments.add("-noprompt");
        arguments.add("-format:detailed");
        addServerArgument(arguments);
        addLoginArgument(arguments);
        return arguments;
    }
   
    /**
     * Parse the data in the reader and return a list of change sets.
     * @param consoleReader console output
     * @return a list of change sets from the console output; empty if none could be found.
     */
    @Override
    public List<TFSLabel> parse(Reader consoleReader) throws ParseException, IOException {
        List<TFSLabel> list = new ArrayList<TFSLabel>();
        LabelsParser parser = new LabelsParser(new BufferedReader(consoleReader));
        while (parser.nextLabel()) {
            if (projectPath.equalsIgnoreCase(parser.getProjectScope())){
                TFSLabel label = new TFSLabel(parser.getLabel(), parser.getProjectScope(), parser.getDate(), parser.getOwner(), parser.getComment());
                list.add(label);
            }
        }
        return list;
    }
}
