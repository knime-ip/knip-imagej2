package org.knime.knip.imagej2.buddydemo;

import imagej.command.Command;
import net.imglib2.type.numeric.RealType;

import org.scijava.ItemIO;
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
 * Notes for buddy class loading
 * 
 * the manifest of org.knime.knip.imagej.core contains the entry "Eclipse-BuddyPolicy: registered" to indicate that it may use the
 * class loaders of registred plugins as well to find classes.
 * 
 * Therefore to use buddy loading we have to add "Eclipse-RegisterBuddy:" with "org.knime.knip.imagej.core" to register the buddydemo plugin
 * and "help" imagej.core with class loading.
 */

@Plugin(menu = {@Menu(label = "DeveloperPlugins"), @Menu(label = "MyBuddyDemoPlugin")}, description = "One way to add new ImageJ plugins to KNIME is to wrap them with eclipse plugins that register themselves as buddies of"
                + " org.knime.knip.imagej.core . The automatic plugin retriev mechanism will discover the plugins, parse the annotations and add them as"
                + " KNIME nodes. However the java compiler settings of the fragment project have to be adjusted to meet the requirements of the sezpoz"
                + " library. For more details see sezpoz.java.net => Notes => Eclipse-specific notes or inspect the MyBuddyDemoPlugin." + "", headless = true, type = Command.class)
public class MyBuddyDemoPlugin<T extends RealType<T>> implements Command {

        @Parameter(type = ItemIO.OUTPUT)
        private String output;

        public void run() {
                output = "plugins can be defined in registerd buddies of imagej.core";
        }

}