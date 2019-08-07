package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.SiriusInputIterator;
import de.unijena.bioinf.babelms.projectspace.*;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.input_provider.InputProvider;
import de.unijena.bioinf.ms.frontend.subtools.input_provider.MzmlInputProvider;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * This is for not algorithm related parameters.
 *
 * That means parameters that do not influence computation and do not
 * need to be Annotated to the MS2Experiment, e.g. standard commandline
 * stuff, technical parameters (cores) or input/output.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 * */
@CommandLine.Command(name = "night-sky", aliases = {"ns"/*, "sirius"*/}, defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, sortOptions = false)
public class RootOptionsCLI implements RootOptions {
    public static final Logger LOG = LoggerFactory.getLogger(RootOptionsCLI.class);

    public enum InputType {PROJECT, SIRIUS, MZML}

    protected final DefaultParameterConfigLoader defaultConfigOptions;

    public RootOptionsCLI(@NotNull DefaultParameterConfigLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
    }


    // region Options: Quality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //todo think how to implement this into the cli???
    // I think a subtool that can be called multiple times could be cool???
    @Option(names = "--noise", description = "Median intensity of noise peaks", order = 10,  hidden = true)
    public Double medianNoise;

    @Option(names = {"--assess-data-quality"}, description = "produce stats on quality of spectra and estimate isolation window. Needs to read all data at once.", order = 20, hidden = true)
    public boolean assessDataQuality;
    //endregion

    // region Options: Basic
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Option(names = "-q", description = "suppress shell output", order = 30)
    public boolean quiet;

    /*@Option(names = "--cite", description = "show citations", order = 40,)
    public boolean cite;*/

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    // region Options: Technical
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Option(names = {"--processors", "--cores"}, description = "Number of cpu cores to use. If not specified Sirius uses all available cores.", order =50)
    public void setNumOfCores(int numOfCores) {
        PropertyManager.setProperty("de.unijena.bioinf.sirius.cpu.cores", String.valueOf(numOfCores));
    }


    @Option(names = "--max-compound-buffer", description = "Maxmimal number of compounds that will be buffered in Memory. A larger buffer ensures that there are enough compounds available to use all cores efficiently during computation. A smaller buffer saves Memory. For Infinite buffer size set it to 0. Default: 2 * --initial_intance_buffer", order = 60)
    private Integer maxInstanceBuffer;

    @Override
    public Integer getMaxInstanceBuffer() {
        initBuffers();
        return maxInstanceBuffer;
    }

    @Option(names = "--initial-compound-buffer", description = "Number of compounds that will be loaded initially into the Memory. A larger buffer ensures that there are enough compounds available to use all cores efficiently during computation. A smaller buffer saves Memory. To load all compounds immediately set it to 0. Default: 2 * --cores", order = 60)
    private Integer initialInstanceBuffer;

    @Override
    public Integer getInitialInstanceBuffer() {
        initBuffers();
        return initialInstanceBuffer;
    }

    private void initBuffers(){
        if (initialInstanceBuffer == null)
            initialInstanceBuffer = SiriusJobs.getGlobalJobManager().getCPUThreads();

        if (maxInstanceBuffer == null) {
            maxInstanceBuffer = initialInstanceBuffer * 2;
        } else {
            if (initialInstanceBuffer <= 0) {
                maxInstanceBuffer = initialInstanceBuffer; //this means infinity
            } else {
                maxInstanceBuffer = Math.max(initialInstanceBuffer, maxInstanceBuffer);
            }
        }
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // region Options: INPUT/OUTPUT
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Option(names = {"--workspace", "-w"}, description = "Specify sirius workspace location. This is the directory for storing Property files, logs, databases and caches.  This is NOT for the project-space that stores the results! Default is $USER_HOME/.sirius", order = 70)
    public Files workspace; //todo change in application core

    @Option(names = "--maxmz", description = "Just consider compounds with a precursor mz lower or equal this maximum mz. All other compounds in the input file are ignored.", order = 100)
    public Double maxMz = Double.POSITIVE_INFINITY;


    @Option(names = "--naming-convention", description = "Specify a format for compounds' output directories. Default %%index_%%filename_%%compoundname", order = 90)
    public void setProjectSpaceFilenameFormatter(String projectSpaceFilenameFormatter) throws ParseException {
        this.projectSpaceFilenameFormatter = new StandardMSFilenameFormatter(projectSpaceFilenameFormatter);
    }

    public FilenameFormatter projectSpaceFilenameFormatter = new StandardMSFilenameFormatter();

    @Option(names = "--recompute", description = "Recompute ALL results of ALL SubTools that are already present. By defaults already present results of an instance will be preserved and the instance will be skipped for the corresponding Task/Tool", order = 95, defaultValue = "FALSE")
    public void setRecompute(boolean recompute) throws Exception {
        try {
            defaultConfigOptions.changeOption("RecomputeResults", String.valueOf((recompute)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @Option(names = {"--output", "--project-space", "-o", "-p"}, description = "Specify project-space to read from and also write to if nothing else is specified. For compression use the File ending .zip or .sirius", order = 70)
    public File projectSpaceLocation;

    @Option(names = {"--input", "-i" }, description = "Input for the analysis. Ths can be either preprocessed mass spectra in .ms or .mgf file format, " +
            "LC/MS runs in .mzml format or already existing SIRIUS project-space(s) (uncompressed/compressed).", order = 80)
    // we differentiate between contiunuing a project-space and starting from mzml or  already processed ms/mgf file.
    // If multiple files match the priobtrrity is project-space,  ms/mgf,  mzml
    public void setInput(List<File> files) {
        if (files == null || files.isEmpty()) return;

        final List<File> projectSpaces = new ArrayList<>();
        final List<File> siriusInfiles = new ArrayList<>();
        final List<File> mzMLInfiles = new ArrayList<>();

        expandInput(files, mzMLInfiles, siriusInfiles, projectSpaces);

        if (!projectSpaces.isEmpty()) {
            if (siriusInfiles.isEmpty() || mzMLInfiles.isEmpty())
                LOG.warn("Multiple input types found: Only the project-space data ist used as input.");
            input = projectSpaces;
            type = InputType.PROJECT;
        } else if (!siriusInfiles.isEmpty()) {
            if (!mzMLInfiles.isEmpty())
                LOG.warn("Multiple input types found: Only the .ms/.mgf data is used as input.");
            input = siriusInfiles;
            type = InputType.SIRIUS;
        } else if (!mzMLInfiles.isEmpty()) {
            input = mzMLInfiles;
            type = InputType.MZML;
        } else {
            throw new CommandLine.PicocliException("No valid input data is found. Please give you input in a supported format.");
        }
    }


    private void expandInput(@NotNull List<File> files, @NotNull final List<File> mzMLInfiles, @NotNull List<File> siriusInfiles, @NotNull List<File> projectSpaces) {
        for (File g : files) {
            if (g.isDirectory()) {
                // check whether it is a workspace or a gerneric directory with som other input
                if (SiriusProjectSpaceIO.isSiriusWorkspaceDirectory(g)) {
                    projectSpaces.add(g);
                } else {
                    File[] ins = g.listFiles(pathname -> pathname.isFile());
                    if (ins != null) {
                        Arrays.sort(ins, Comparator.comparing(File::getName));
                        expandInput(Arrays.asList(ins), mzMLInfiles, siriusInfiles, projectSpaces);
                    }
                }
            } else {
                //check whether files are lcms runs copressed project-spaces or stadard ms/mgf files
                final String name = g.getName();
                if (MsExperimentParser.isSupportedFileName(name)) {
                    siriusInfiles.add(g);
                } else if (SiriusProjectSpaceIO.isCompressedProjectSpaceName(name)) {
                    projectSpaces.add(g);
                } else if (name.toLowerCase().endsWith(".mzml")) {
                    //todo add mzML support?
                    LOG.warn("Mzml file found. This format is currently not supported but support is planned for future releases. File is skipped.");
                } else {
                    LOG.warn("File with the name \"" + name + "\" is not in a supported format or has a wrong file extension. File is skipped");
                }
            }
        }
    }

    List<File> input = null;
    InputType type = null;


    @Option(names = {"--ignore-formula" }, description = "ignore given molecular formula in .ms or .mgf file format, ")
    private boolean ignoreFormula = false;
    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    private SiriusProjectSpace projectSpaceToWriteOn = null;
    @Override
    public SiriusProjectSpace getProjectSpace() {
        if (projectSpaceToWriteOn == null)
            configureProjectSpace();

        return projectSpaceToWriteOn;
    }

    @Override
    public InputProvider getInputProvider() {
        if (projectSpaceToWriteOn == null)
            configureProjectSpace();

        if (type != null && input != null) {
            switch (type) {
                case PROJECT:
                    return () -> projectSpaceToWriteOn.parseExperimentIterator();
                case SIRIUS:
                    if (projectSpaceToWriteOn.getNumberOfWrittenExperiments() > 0)
                        return () -> SiriusProjectSpaceIO.readInputAndProjectSpace(input, projectSpaceToWriteOn, maxMz, ignoreFormula);
                    else
                        return () -> new SiriusInputIterator(input, maxMz, ignoreFormula).asExpResultIterator();
                case MZML:
                    return new MzmlInputProvider(input);
            }
        } else if (projectSpaceToWriteOn != null && projectSpaceToWriteOn.getNumberOfWrittenExperiments() > 0) {
            LOG.info("No Input given but output Project-Space is not empty and will be used as Input instead!");
            return () -> projectSpaceToWriteOn.parseExperimentIterator();
        }
        throw new CommandLine.PicocliException("Illegal Input type: " + type);
    }


    protected void configureProjectSpace() {
        try {
            if (type == InputType.PROJECT) {
                if (projectSpaceLocation == null) {
                    if (input.size() == 1)
                        projectSpaceLocation = input.get(0);
                    else
                        throw new CommandLine.PicocliException("No output location given. Can only be avoided if a singe project-space it the input");
                }

                projectSpaceToWriteOn = SiriusProjectSpaceIO.create(projectSpaceLocation, input, projectSpaceFilenameFormatter,
                        (currentProgress, maxProgress, Message) -> {
                            System.out.println("Creating Project Space: " + (((((double) currentProgress) / (double) maxProgress)) * 100d) + "%");
                        }
                        , makeSerializerArray());
            } else if (type == InputType.MZML) {
                //todo implement
                throw new CommandLine.PicocliException("MZML input is not yet supported! This should not be possible. BUG?");
            } else {
                projectSpaceToWriteOn = SiriusProjectSpaceIO.create(projectSpaceFilenameFormatter, projectSpaceLocation,
                        (currentProgress, maxProgress, Message) -> {
                            System.out.println("Creating Project Space: " + (((((double) currentProgress) / (double) maxProgress)) * 100d) + "%");
                        }
                        , makeSerializerArray());

                if (projectSpaceToWriteOn.getNumberOfWrittenExperiments() > 0)
                    LOG.info("Output Project-Space is not empty. It will be merged with the provided input!");
            }

            projectSpaceToWriteOn.registerSummaryWriter(new MztabSummaryWriter());
        } catch (IOException e) {
            throw new CommandLine.PicocliException("Could not initialize workspace!", e);
        }
    }


    protected MetaDataSerializer[] makeSerializerArray() {

        //TODO this should be collected from the different subtool clis
        // we should be able to import and export the data even if
        // the calculators are not available -> e.g. web connection!
        List<MetaDataSerializer> al = new ArrayList<>();
        al.add(new IdentificationResultSerializer());
        al.add(new ZodiacResultSerializer());
        al.add(new FingerIdResultSerializer(ApplicationCore.WEB_API));
        if (ApplicationCore.CANOPUS != null)
            al.add(new CanopusResultSerializer(ApplicationCore.CANOPUS));
        al.add(new PassatuttoSerializer());

        return al.toArray(new MetaDataSerializer[0]);
    }
}
