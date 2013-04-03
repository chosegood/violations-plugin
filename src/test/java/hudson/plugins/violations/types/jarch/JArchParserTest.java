/*
 * Copyright 2013 Corelogic Ltd All Rights Reserved.
 */
package hudson.plugins.violations.types.jarch;

import static org.junit.Assert.*;

import hudson.plugins.violations.ViolationsParser;
import hudson.plugins.violations.ViolationsParserTest;
import hudson.plugins.violations.model.FullBuildModel;

import java.io.IOException;

import org.junit.Test;

public class JArchParserTest extends ViolationsParserTest {

    protected FullBuildModel getFullBuildModel(String filename) throws IOException {
        ViolationsParser parser = new JArchParser();
        return getFullBuildModel(parser, filename);
    }

    @Test
    public void testParseWithSingleFile() throws Exception {
        FullBuildModel model = getFullBuildModel("oneruleset.xml");

        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        System.out.println(model);
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

        // check number of violations and number of files
        assertEquals(2, model.getCountNumber(JArchParser.TYPE_NAME));
        assertEquals(1, model.getFileModelMap().size());
    }

}
