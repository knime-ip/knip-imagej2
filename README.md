KNIME ImageJ2 Integration
============

The KNIP ImageJ2 plugin provides support for ImageJ1 macro execution and ImageJ2 plugins. The latter
allows to write an algorithm once using the ImageJ2 plugin language and execute it in KNIME and ImageJ2.

#### ImageJ1 Macro node (Beta)

The integration of ImageJ1 plugins into KNIME is difficult because they are often not intended for headless execution.  However the macro language of ImageJ1 allows to execute plugins by name and the ImageJ Macro node uses this entry point to provide access to some preselected plugins.

These plugins can be executed without GUI and are therefore suitable for the integration into KNIME workflows. The configuration dialog provides access to the plugins and the plugin parameterization. Additionally it supports the chained execution of multiple macros and the selection of the processed image dimensions (i.e. execution of 2D plugins on 3D data...). Finally "pure" macro language code can be executed. However this feature is intended for advanced users and a successful execution of the code can not be guaranteed.

#### ImageJ2 Integration (Beta)

ImageJ2 introduces an extension framework for algorithms with clearly defined inputs and outputs. This allows us to automatically detect plugins in order to wrap them in full-fledged KNIME nodes. From a user perspective such a node can be used like any other node. The configuration of the parameters can be accessed via the configuration dialog, the node has (normally) an input port that should be connected to an image source and an output port that provides the processed result.

Apart from the standard parametrization via the configuration dialog, algorithm parameters can also be bound to table columns. Thus advanced workflows with different prametrizations for each image become available for high-throughput or batch processing.

We currently support a basic set of input and output parameters that can be extended by core or third party developers as needed. These parameters get converted with adapters that translate the table centric data model of KNIME to ImageJ2 and vice versa. However, not all plugins can be executed with KNIME, apart from suitable parameter annotations and adapters it is very important, that a plugin supports headless execution such that it fits the "configure once execute often" paradigm of KNIME.

The ImageJ2 plugin comes with some pre installed example plugins , like edge detection or the ImageJ2 shadow plugins, that demonstrate the neat integration of KNIME and ImageJ2. Additionally an ImageJ2 version of Tubeness 1.2 (http://www.longair.net/edinburgh/imagej/tubeness/) has been included as a demonstration of a more advanced plugin (use grayscale images to test it). Most importantly,  ImageJ2 plugins can easily be added to KNIME via KNIME update sites or with the local installations of the plugins (mainly intended for development purposes).  To test the local installation mode go to the Image Processing Preference Page (File -> Preferences -> KNIME -> Image Processing Plugin) and select ImageJ2 Plugin Installation, then choose an ImageJ2 plugin jar-file (with sezpoz annotations), install it and restart KNIME. The plugins become available in the local node repository according to the menu annotations of the plugin. 

The KNIME integration of ImageJ2 is currently a beta release but with the ongoing development of ImageJ2 we hope to improve the integration between both tools. However, the current version already allows to write algorithms, that run in KNIME and ImageJ2 at the same time, without requiring a deeper knowledge of the KNIME API.
