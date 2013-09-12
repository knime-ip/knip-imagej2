import imagej.command.Command;
import net.imglib2.img.Img;

import org.scijava.ItemIO;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(menu = {@Menu(label = "DeveloperPlugins"), @Menu(label = "imgTEst")}, description = "One way to add new ImageJ plugins to KNIME is to wrap them with fragments of org.knime.knip.imagej.core . The"
                + " automatic plugin retriev mechanism will discover the plugins, parse the annotations and add them as KNIME nodes. However the"
                + " java compiler settings of the fragment project have to be adjusted to meet the requirements of the sezpoz library. For more details"
                + " see sezpoz.java.net => Notes => Eclipse-specific notes or inspect the MyFragmentDemoPlugin.", headless = true, type = Command.class)
public class ImgTest implements Command {

        @Parameter(type = ItemIO.INPUT)
        private String bla;

        @Parameter(type = ItemIO.BOTH)
        private Img inout;

        public void run() {

        }

}