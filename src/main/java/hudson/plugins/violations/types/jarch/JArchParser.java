package hudson.plugins.violations.types.jarch;

import org.xmlpull.v1.XmlPullParserException;

import hudson.plugins.violations.model.FullFileModel;
import hudson.plugins.violations.model.Severity;
import hudson.plugins.violations.model.Violation;
import hudson.plugins.violations.parse.AbstractTypeParser;

import java.io.IOException;

public class JArchParser extends AbstractTypeParser {

    static final String TYPE_NAME = "jarch";

    
    /**
     * Parse the JSLint xml file.
     * @throws IOException if there is a problem reading the file.
     * @throws XmlPullParserException if there is a problem parsing the file.
     */
    protected void execute() throws IOException, XmlPullParserException {

        // ensure that the top level tag is "jarch"
        expectNextTag("jarch");
        getParser().next(); // consume the "jarch" tag

        // loop thru the child elements, getting the "file" ones
        while (skipToTag("ruleset")) {
            parseRulesetElement();
        }
    }

    /*
    <ruleset name="component-dependencies" >
        <violation
            message="MODULE: 'configuration' must not import from 'application'"
            class="uk.co.corelogic.mosaic.configuration.admin.controller.AdminController"
            lineNumber="16"
            line="import uk.co.corelogic.mosaic.application.common.log.Loggable; "/>
    </ruleset>
    */

    private void parseRulesetElement() throws IOException, XmlPullParserException {

        String rulesetName = fixAbsolutePath(checkNotBlank("name"));
        getParser().next(); // consume "file" tag
        FullFileModel fileModel = getFileModel(rulesetName);

        // loop thru the child elements, getting the "issue" ones
        while (skipToTag("issue")) {
            fileModel.addViolation(parseViolationElement());
        }
        endElement();
    }

    private Violation parseViolationElement()
        throws IOException, XmlPullParserException {
        Violation violation = new Violation();
        violation.setType("jarch");
        violation.setLine(getParser().getAttributeValue("", "lineNumber"));
        violation.setMessage(getParser().getAttributeValue("", "message"));
        violation.setPopupMessage(getParser().getAttributeValue("", "line"));
        violation.setSource(getParser().getAttributeValue("", "class"));
        violation.setSeverity(Severity.MEDIUM);
        getParser().next();
        endElement(); // violation element
        return violation;
    }
}