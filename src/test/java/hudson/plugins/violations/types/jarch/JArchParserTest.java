package hudson.plugins.violations.types.jarch;

import hudson.plugins.violations.ViolationsParser;
import hudson.plugins.violations.ViolationsParserTest;
import hudson.plugins.violations.model.FullBuildModel;

import java.io.IOException;
import java.util.logging.Logger;

import org.junit.Test;

public class JArchParserTest extends ViolationsParserTest {

    static final Logger logger = Logger.getLogger(JArchParserTest.class.toString());

    @Override
    protected FullBuildModel getFullBuildModel(String filename) throws IOException {
        ViolationsParser parser = new JArchParser();
        return getFullBuildModel(parser, filename);
    }

    @Test
    public void testParseWithSingleRuleset() throws Exception {
//        FullBuildModel model = getFullBuildModel("oneruleset.xml");

        // check number of violations and number of files
        // assertEquals(2, model.getCountNumber(JArchParser.TYPE_NAME));
        // assertEquals(1, model.getFileModelMap().size());
    }

    @Test
    public void testParseWithMultipleRuleset() throws Exception {
//        FullBuildModel model = getFullBuildModel("jarch.xml");

        // check number of violations and number of files
        // assertEquals(4, model.getCountNumber(JArchParser.TYPE_NAME));
        // assertEquals(2, model.getFileModelMap().size());
    }

}
