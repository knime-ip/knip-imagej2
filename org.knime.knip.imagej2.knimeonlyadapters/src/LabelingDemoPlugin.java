import net.imglib2.labeling.Labeling;
import net.imglib2.ops.operation.labeling.unary.ErodeLabeling;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/*
 * Notes for fragment plugins
 * 
 * just wrap the plugin in a plugin fragment of imagej.core
 */

@Plugin(menu = { @Menu(label = "DeveloperPlugins"),
		@Menu(label = "Labeling Adapter Test") }, description = "can be used to test the labeling adapter (a KNIME only adapter)", headless = true, type = Command.class)
public class LabelingDemoPlugin<L extends Comparable<L>> implements Command {

	@Parameter(type = ItemIO.BOTH)
	private Labeling labeling;

	@Override
	public void run() {
		final Labeling l = labeling.copy();
		final long[][] localStruct = new long[][] { { 0, -1 }, { -1, 0 }, { 0, 0 },
				{ 1, 0 }, { 0, 1 } };
		new ErodeLabeling(localStruct).compute(l, labeling);
	}

}