/*
 * Copyright 2013 Corelogic Ltd All Rights Reserved.
 */
package hudson.plugins.violations.types.jarch;

import hudson.plugins.violations.ViolationsParser;
import hudson.plugins.violations.model.FullBuildModel;
import hudson.plugins.violations.model.FullFileModel;
import hudson.plugins.violations.model.Severity;
import hudson.plugins.violations.model.Violation;
import hudson.plugins.violations.parse.ParseUtil;
import hudson.plugins.violations.util.AbsoluteFileFinder;
import hudson.util.IOException2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class JArchParser implements ViolationsParser {

    private static final Logger logger = Logger.getLogger(JArchParser.class.getName());

    static final String TYPE_NAME = "jarch";
    private FullBuildModel model;
    private File projectPath;
    private String[] sourcePaths;

    /** {@inheritDoc} */
    public void parse(FullBuildModel model, File projectPath, String fileName, String[] sourcePaths) throws IOException {
        logger.log(Level.INFO, "Starting jArch parsing");

        this.model = model;
        this.projectPath = projectPath;
        this.sourcePaths = sourcePaths;

        AbsoluteFileFinder absoluteFileFinder = new AbsoluteFileFinder();
        absoluteFileFinder.addSourcePath(this.projectPath.getPath());
        if (this.sourcePaths != null) {
            absoluteFileFinder.addSourcePaths(this.sourcePaths);
        }

        logger.log(Level.INFO, "Project Path: " + this.projectPath);
        logger.log(Level.INFO, "Filename: " + fileName);
        for (String source : this.sourcePaths) {
            logger.log(Level.INFO, "Source: " + source);
        }

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;

        try {
            docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new FileInputStream(new File(projectPath, fileName)));

            NodeList ruleSets = doc.getElementsByTagName("ruleset");
            for (int i = 0; i < ruleSets.getLength(); i++) {
                final Node ruleSet = ruleSets.item(i);
                final String rulesetName = ruleSet.getAttributes().getNamedItem("name").getNodeValue();
                logger.log(Level.INFO, "Parsing Rulesset: " + rulesetName);

                NodeList jarchviolations = ruleSet.getChildNodes();
                for (int j = 0; j < jarchviolations.getLength(); j++) {
                    final Node jarchViolation = jarchviolations.item(j);
                    NamedNodeMap attributesList = jarchViolation.getAttributes();

                    if (attributesList != null) {
                        String messageAttribute = attributesList.getNamedItem("message") != null ? attributesList
                                .getNamedItem("message").getNodeValue() : null;
                        String fileAttribute = attributesList.getNamedItem("file") != null ? attributesList
                                .getNamedItem("file").getNodeValue() : null;
                        String lineNumberAttribute = attributesList.getNamedItem("lineNumber") != null ? attributesList
                                .getNamedItem("lineNumber").getNodeValue() : null;
                        String lineAttribute = attributesList.getNamedItem("line") != null ? attributesList
                                .getNamedItem("line").getNodeValue() : null;
                        String classAttribute = attributesList.getNamedItem("class") != null ? attributesList
                                .getNamedItem("class").getNodeValue() : null;
                        String typeAttribute = attributesList.getNamedItem("type") != null ? attributesList
                                .getNamedItem("type").getNodeValue() : null;
                        addViolation(absoluteFileFinder, messageAttribute, lineNumberAttribute, lineAttribute, classAttribute, typeAttribute, fileAttribute);
                    }
                }

            }

        } catch (ParserConfigurationException pce) {
            throw new IOException2(pce);
        } catch (SAXException se) {
            throw new IOException2(se);
        }
    }

    void addViolation(final AbsoluteFileFinder absoluteFileFinder, final String messageAttribute, final String lineNumberAttribute,
            final String lineAttribute, final String classAttribute, final String typeAttribute, final String fileAttribute) {

        String classPath= resolveFullClassName(classAttribute);
        logger.log(Level.INFO, "Adding violation for class[" + classPath + "]");

        File sourceFile = absoluteFileFinder.getFileForName(fileAttribute);
        String className = getRelativeName(classPath, sourceFile);
        logger.log(Level.FINE, "Resolved classAttribute[" + classAttribute + "] with file[" + sourceFile.getName() + "] as [" + className + "]");

        Violation violation = new Violation();
        violation.setLine(lineNumberAttribute);
        violation.setMessage(messageAttribute);
        violation.setPopupMessage(messageAttribute);
        violation.setSource(lineAttribute);
        violation.setType(TYPE_NAME);
        if (typeAttribute!= null && typeAttribute.equals("LAYER")) {
            violation.setSeverity(Severity.HIGH);
            violation.setSeverityLevel(Severity.HIGH_VALUE);
        } else {
            violation.setSeverity(Severity.MEDIUM);
            violation.setSeverityLevel(Severity.MEDIUM_VALUE);
        }

        FullFileModel fileModel = this.model.getFileModel(classPath);
        if (sourceFile != null && sourceFile.exists()) {
            logger.log(Level.FINE, "Source File for [" + classPath + "] Source[" + sourceFile.getAbsolutePath() + "] lastModified[" + sourceFile.lastModified() + "]");
            fileModel.setSourceFile(sourceFile);
            fileModel.setLastModified(sourceFile.lastModified());
            logger.log(Level.FINE, "fileModel.getSourceFile() : " + fileModel.getSourceFile().getAbsolutePath());
            logger.log(Level.FINE, "fileModel.getDisplayName() : " + fileModel.getDisplayName());
        } else {
            logger.log(Level.WARNING, "Source File for [" + classPath + "] not found");
        }
        fileModel.addViolation(violation);
    }

    String resolveFullClassName(String classname) {
        if (classname == null) {
            return null;
        }
        String filename = classname.replace(".", File.separator);
        int innerClassPosition = filename.indexOf('$');
        if (innerClassPosition != -1) {
            filename = filename.substring(0, innerClassPosition);
        }

        filename = filename+ ".java";
        return filename; 
    }

    private String getRelativeName(String name, File file) {
        if (file != null && file.exists()) {
            String absolute = file.getAbsolutePath();
            String relative = resolveName(absolute);
            if (!relative.equals(absolute)) {
                return relative;
            }
        }
        return name;
    }

    /**
     * Resolve an absolute name agaist the project path.
     * @param absoluteName the absolute name.
     * @return a path relative to the project path or an absolute
     *         name if the name cannot be resolved.
     */
    protected String resolveName(String absoluteName) {
        return ParseUtil.resolveAbsoluteName(projectPath, absoluteName);
    }

}
