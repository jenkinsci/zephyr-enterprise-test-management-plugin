package com.thed.model;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * @author Mohan.Kumar
 * This stores the results of an eggPlant execution
 */
public class EggPlantResult implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -7436928706318596861L;

    //format for RunDate
    private SimpleDateFormat format= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");

    /** script */
    private String script;
    
    /** sut */
    private String sut;

    /** isPassed */
    private boolean isPassed;

    /** runDate */
    private String runDate;

    /** Duration */
    private String Duration;

    /** errors */
    private String errors;
    
    /** warnings */
    private String warnings;
    
    /** exceptions */
    private String  exceptions;

    /** ScriptLines */
    private ArrayList<EggPlantScriptLine> ScriptLines;
    
    /** xmlResultFile */
    private String xmlResultFile;

    public String getSut() {
        return sut;
    }

    public void setSut(String sut) {
        this.sut = sut;
    }

    public String getDuration() {
        return Duration;
    }

    public void setDuration(String Duration) {
        this.Duration = Duration;
    }

    public String getErrors() {
        return errors;
    }

    public void setErrors(String errors) {
        this.errors = errors;
    }

    public String getExceptions() {
        return exceptions;
    }

    public void setExceptions(String exceptions) {
        this.exceptions = exceptions;
    }

    public String getRunDate() {
        return runDate;
    }

    public void setRunDate(String runDate) {
        this.runDate = runDate;
    }

    public String getWarnings() {
        return warnings;
    }

    public void setWarnings(String warnings) {
        this.warnings = warnings;
    }


    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    /**
     * @return isPassed
     */
    public boolean isPassed() {
        return isPassed;
    }
    
    public void setPassed(boolean isPassed) {
        this.isPassed = isPassed;
    }

    public void addScriptLine(EggPlantScriptLine esl)
    {
        if (ScriptLines == null)
        {
            ScriptLines = new ArrayList<EggPlantScriptLine>();
        }
        ScriptLines.add(esl);
    }

    /**
     * @return ScriptLines
     */
    public ArrayList<EggPlantScriptLine> getScriptLines() {
        return ScriptLines;
    }

    /**
     * @param scriptlines ScriptLines
     */
    public void setScriptLines(ArrayList<EggPlantScriptLine> scriptlines) {
        this.ScriptLines = scriptlines;
    }

	public String getXmlResultFile() {
		return xmlResultFile;
	}

	public void setXmlResultFile(String xmlResultFile) {
		this.xmlResultFile = xmlResultFile;
	}

    public Date getRunDateInDate() throws ParseException {
        return format.parse(getRunDate());
    }

}
