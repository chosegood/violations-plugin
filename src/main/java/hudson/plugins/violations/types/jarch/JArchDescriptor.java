package hudson.plugins.violations.types.jarch;

import hudson.plugins.violations.TypeDescriptor;
import hudson.plugins.violations.ViolationsParser;

import java.util.ArrayList;
import java.util.List;

public class JArchDescriptor
    extends TypeDescriptor {

    /** The descriptor for the jArch violations type. */
    public static final JArchDescriptor DESCRIPTOR
        = new JArchDescriptor();

    private JArchDescriptor() {
        super("jarch");
    }

    /**
     * Create a parser for the checkstyle type.
     * @return a new checkstyle parser.
     */
    @Override
    public ViolationsParser createParser() {
        return new JArchParser();
    }

    /**
     * Get a list of target xml files to look for
     * for this particular type.
     * @return a list filenames to look for in the target
     *         target directory.
     */
    @Override
    public List<String> getMavenTargets() {
        List<String> ret = new ArrayList<String>();
        ret.add("jarch-result.xml");
        return ret;
    }

}