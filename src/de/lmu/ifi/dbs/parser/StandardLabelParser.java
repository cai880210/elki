package de.lmu.ifi.dbs.parser;

import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Provides a parser for parsing one point per line, attributes separated by whitespace.
 * 
 * Several labels may be given per point. A label must not be parseable as double.
 * Lines starting with &quot;#&quot; will be ignored.
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class StandardLabelParser extends NormalizingParser<DoubleVector>
{
    /**
     * The comment character.
     */
    public static final String COMMENT = "#";
    
    /**
     * A sign to separate components of a label.
     */
    public static final String LABEL_CONCATENATION = " ";
    
    /**
     * A pattern defining whitespace.
     */
    public static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /**
     * Provides a parser for parsing one point per line, attributes separated by whitespace.
     * 
     * Several labels may be given per point. A label must not be parseable as double.
     * Lines starting with &quot;#&quot; will be ignored.
     *
     */
    public StandardLabelParser()
    {
        super();
    }
    
    /**
     * 
     * @see de.lmu.ifi.dbs.parser.Parser#parse(java.io.InputStream)
     */
    public Database<DoubleVector> parse(InputStream in)
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        int lineNumber = 0;
        int dimensionality = -1;
        List<DoubleVector> objects = new ArrayList<DoubleVector>();
        List<Map<AssociationID,Object>> labels = new ArrayList<Map<AssociationID,Object>>();
        try
        {
            for(String line; (line=reader.readLine())!=null; lineNumber++)
            {
                if(!line.startsWith(COMMENT) && line.length() > 0)
                {
                    String[] entries = WHITESPACE.split(line);
                    List<Double> attributes = new ArrayList<Double>();
                    StringBuffer label = new StringBuffer();
                    for(int i = 0; i < entries.length; i++)
                    {
                        try
                        {
                            Double attribute = Double.valueOf(entries[i]);
                            attributes.add(attribute);
                        }
                        catch(NumberFormatException e)
                        {
                            if(label.length() > 0)
                            {
                                label.append(LABEL_CONCATENATION);
                            }
                            label.append(entries[i]);
                        }
                    }
                    if(dimensionality < 0)
                    {
                        dimensionality = attributes.size();
                    }
                    if(dimensionality != attributes.size())
                    {
                        throw new IllegalArgumentException("Differing dimensionality in line "+lineNumber+".");
                    }
                    objects.add(new DoubleVector(attributes));
                    Map<AssociationID,Object> associationMap = new Hashtable<AssociationID,Object>();
                    Object association;
                    if(classLabel==null)
                    {
                        association = label.toString();
                    }
                    else
                    {
                        try
                        {
                            association = Class.forName(classLabel).newInstance();
                            ((ClassLabel) association).init(label.toString());
                        }
                        catch(InstantiationException e)
                        {
                            throw new IllegalStateException(e);
                        }
                        catch(IllegalAccessException e)
                        {
                            throw new IllegalStateException(e);
                        }
                        catch(ClassNotFoundException e)
                        {
                            throw new IllegalStateException(e);
                        }                        
                    }
                    associationMap.put(associationID,association);
                    labels.add(associationMap);
                }                
            }            
        }
        catch(IOException e)
        {
            throw new IllegalArgumentException("Error while parsing line "+lineNumber+".");
        }
        if(getNormalization() != null)
        {
            try
            {
                objects = getNormalization().normalize(objects);
            }
            catch(NonNumericFeaturesException e)
            {
                e.printStackTrace();
            }
        }
        try
        {
            database.insert(objects,labels);
        }
        catch(UnableToComplyException e)
        {
            e.printStackTrace();
        }
        return database;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    public String description()
    {
        StringBuffer description = new StringBuffer();
        description.append(StandardLabelParser.class.getName());
        description.append(" expects following format of parsed lines:\n");
        description.append("A single line provides a single point. Attributes are separated by whitespace (");
        description.append(WHITESPACE.pattern());
        description.append("). Any substring not containing whitespace is tried to be read as double. If this fails, it will be appended to a label. (Thus, any label must not be parseable as double.) Empty lines and lines beginning with \"");
        description.append(COMMENT);
        description.append("\" will be ignored. If any point differs in its dimensionality from other points, the parse method will fail with an Exception.\n");
        
        return usage(description.toString());
    }

}
