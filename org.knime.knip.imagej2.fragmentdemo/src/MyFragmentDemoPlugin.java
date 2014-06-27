import net.imglib2.type.numeric.RealType;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/* Notes from SezPoz.java.net
 *
 * Notes
 *
 * Known limitations:
 *
 * When using JDK 5 and apt, incremental compilation can result in an index file being generated with only some of the desired entries,
 * if other source files are omitted e.g. by Ant. This scenario works better using JDK 6's javac: if you compile just some sources which
 * are marked with an indexable annotation, these entries will be appended to any existing registrations from previous runs of the compiler.
 * (You should run a clean build if you delete annotations from sources.)
 *
 * The Java language spec currently prohibits recursive annotation definitions, although javac in JDK 5 does not. (JDK 6 and 7's javac do.)
 * See bug #6264216.
 *
 * Eclipse-specific notes: make sure annotation processing is enabled at least for any projects registering objects using annotations.
 * Make sure the SezPoz library is in the factory path for annotation processors. You also need to check the box Run this container's processor
 * in batch mode from the Advanced button in Java Compiler > Annotation Processing > Factory Path. There does not appear to be any way for Eclipse
 * to discover processors in the regular classpath as JSR 269 suggests, and there does not appear to be any way to make these settings apply
 * automatically to all projects. Eclipse users are recommended to use javac (e.g. via Maven) to build. Eclipse Help Page Eclipse bug #280542
 *
 */

/*
 * Notes for fragment plugins
 * 
 * just wrap the plugin in a plugin fragment of imagej.core
 */

@Plugin(menu = {@Menu(label = "DeveloperPlugins"), @Menu(label = "MyFragmentDemoPlugin")}, description = "One way to add new ImageJ plugins to KNIME is to wrap them with fragments of org.knime.knip.imagej.core . The"
                + " automatic plugin retriev mechanism will discover the plugins, parse the annotations and add them as KNIME nodes. However the"
                + " java compiler settings of the fragment project have to be adjusted to meet the requirements of the sezpoz library. For more details"
                + " see sezpoz.java.net => Notes => Eclipse-specific notes or inspect the MyFragmentDemoPlugin.", headless = true, type = Command.class)
public class MyFragmentDemoPlugin<T extends RealType<T>> implements Command {

        @Parameter(type = ItemIO.OUTPUT)
        private String output;

        @Override
        public void run() {
                output = "plugins can be defined as fragments of imagej.core";
        }

}