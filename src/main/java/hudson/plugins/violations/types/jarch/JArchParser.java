/*
 * Copyright 2013 Corelogic Ltd All Rights Reserved.
 */
package hudson.plugins.violations.types.jarch;

import hudson.plugins.violations.ViolationsParser;
import hudson.plugins.violations.model.FullBuildModel;
import hudson.plugins.violations.model.FullFileModel;
import hudson.plugins.violations.model.Violation;
import hudson.plugins.violations.types.fxcop.XmlElementUtil;
import hudson.plugins.violations.util.AbsoluteFileFinder;
import hudson.util.IOException2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jfree.util.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class JArchParser implements ViolationsParser {

    private final static Logger logger = Logger.getLogger(JArchParser.class.toString());

    static final String TYPE_NAME = "jarch";
    private FullBuildModel model;
    private File reportParentFile;
    private File projectPath;
    private String[] sourcePaths;

    /** {@inheritDoc} */
    public void parse(FullBuildModel model, File projectPath, String fileName, String[] sourcePaths) throws IOException {
        logger.info("Starting jArch parsing");

        this.projectPath = projectPath;
        this.model = model;
        this.reportParentFile = new File(fileName).getParentFile();
        this.sourcePaths = sourcePaths;

        AbsoluteFileFinder finder = new AbsoluteFileFinder();
        finder.addSourcePath(this.projectPath.getPath());
        if (this.sourcePaths != null) {
            finder.addSourcePaths(this.sourcePaths);
        }
        finder.addSourcePath(projectPath.getPath() + "/src/mosaic/main/java/");

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;

        try {
            docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new FileInputStream(new File(projectPath, fileName)));

            NodeList ruleSets = doc.getElementsByTagName("ruleset");
            for (int i = 0; i < ruleSets.getLength(); i++) {
                final Node ruleSet = ruleSets.item(i);
                final String rulesetName = ruleSet.getAttributes().getNamedItem("name").getNodeValue();
                logger.info(rulesetName);

                NodeList jarchviolations = ruleSet.getChildNodes();
                for (int j = 0; j < jarchviolations.getLength(); j++) {
                    final Node jarchViolation = jarchviolations.item(j);
                    NamedNodeMap attributesList = jarchViolation.getAttributes();

                    if (attributesList != null) {
                        String messageAttribute = attributesList.getNamedItem("message") != null
                                ? attributesList.getNamedItem("message").getNodeValue()
                                        : null;
                        String lineNumberAttribute = attributesList.getNamedItem("lineNumber") != null
                                ? attributesList.getNamedItem("lineNumber").getNodeValue()
                                        : null;
                        String lineAttribute = attributesList.getNamedItem("line") != null
                                ? attributesList.getNamedItem("line").getNodeValue() : null;
                        String classAttribute = attributesList.getNamedItem("class") != null
                                ? attributesList.getNamedItem("class").getNodeValue()
                                        : null;

                        addViolation(finder, messageAttribute, lineNumberAttribute, lineAttribute, classAttribute);
                    }
                }

            }

            // parse each violations
            // parseViolations(XmlElementUtil.getNamedChildElements(resultsElement,
            // "rule"));

        } catch (ParserConfigurationException pce) {
            throw new IOException2(pce);
        } catch (SAXException se) {
            throw new IOException2(se);
        }
    }

    private void addViolation(AbsoluteFileFinder finder, String messageAttribute, String lineNumberAttribute, String lineAttribute, String classAttribute) {
        Violation violation = new Violation();
        violation.setLine(lineNumberAttribute);
        violation.setMessage(messageAttribute);
        violation.setPopupMessage(messageAttribute);
        violation.setSource(lineAttribute);
        violation.setType(TYPE_NAME);

        String classFileName = resolveFullClassName(classAttribute);

        Log.info("Class[" + classFileName + "]");
        FullFileModel fileModel = this.model.getFileModel(classFileName);
        File sourceFile = finder.getFileForName(classFileName);
        if (sourceFile != null && sourceFile.exists()) {
            fileModel.setSourceFile(sourceFile);
            fileModel.setLastModified(sourceFile.lastModified());
        }
        fileModel.addViolation(violation);
    }

    String resolveFullClassName(String classname) {
        if (classname == null) {
            return null;
        }
        String filename = classname.replace(".", File.separator);
        return filename + ".java";
    }

    String resolveClassName(String classname) {
        if (classname == null) {
            return null;
        }

        String filename = resolveFullClassName(classname);
        int pos = filename.lastIndexOf("/");
        if (pos != -1) {
            filename = filename.substring(pos + 1);
        }
        return filename;
    }

    private FullFileModel getFileModel(FullBuildModel model, String name, File sourceFile) {
        FullFileModel fileModel = model.getFileModel(name);
        File other = fileModel.getSourceFile();

        if (sourceFile == null || ((other != null) && (other.equals(sourceFile) || other.exists()))) {
            return fileModel;
        }

        fileModel.setSourceFile(sourceFile);
        fileModel.setLastModified(sourceFile.lastModified());
        return fileModel;
    }

}
