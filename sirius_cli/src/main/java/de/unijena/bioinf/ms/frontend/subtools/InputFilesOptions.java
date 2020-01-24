package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.frontend.io.InstanceImporter;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InputFilesOptions {

    public Stream<Path> getAllFilesStream() {
        if (msInput == null)
            return Stream.of();
        return Stream.of(msInput.msParserfiles, msInput.projects, msInput.unknownFiles).flatMap(Collection::stream);
    }

    public List<Path> getAllFiles() {
        return getAllFilesStream().collect(Collectors.toList());
    }

    public File[] getAllFilesArray() {
        return getAllFilesStream().map(Path::toFile).toArray(File[]::new);
    }

    @CommandLine.ArgGroup(exclusive = false, heading = "@|bold Specify multi-compound inputs (.ms, .mgf, .mzML/.mzXml, .sirius):%n|@", order = 320)
    public /*final*/ MsInput msInput;


    public static class MsInput {
        public final List<Path> msParserfiles, projects, unknownFiles;

        public MsInput() {
            this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        public MsInput(List<Path> msParserfiles, List<Path> projects, List<Path> unknownFiles) {
            this.msParserfiles = msParserfiles;
            this.projects = projects;
            this.unknownFiles = unknownFiles;
        }

        @CommandLine.Option(names = {"--input", "-i"}, description = "Specify the input in multi-compound input formats: Preprocessed mass spectra in .ms or .mgf file format, " +
                "LC/MS runs in .mzML/.mzXml format or already existing SIRIUS project-spaces (uncompressed/compressed) but also any other file type e.g. to provide input for STANDALONE tools.", required = true, split = ",", order = 321)
        protected void setInputPath(List<Path> files) {
            msParserfiles.clear();
            projects.clear();
            unknownFiles.clear();
            InstanceImporter.expandInput(files, this);
        }

        @CommandLine.Option(names = {"--ignore-formula"}, description = "ignore given molecular formula if present in .ms or .mgf input files.", defaultValue = "false", order = 322)
        public boolean ignoreFormula;
    }

    // region Options: CSV Input
    @CommandLine.ArgGroup(exclusive = false, multiplicity = "0..*", heading = "@|bold Specify generic inputs (CSV) on per compound level:%n|@", order = 330)
    public List<CsvInput> csvInputs;

    public static class CsvInput {
        @CommandLine.Option(names = {"-1", "--ms1"}, description = "MS1 spectra files", paramLabel = "<ms1File>[,<ms1File>...]", order = 331)
        protected void setMs1(String ms1Files){
            this.ms1 = Arrays.stream(ms1Files.split(",")).map(File::new).collect(Collectors.toList());
        }
        public List<File> ms1;

        @CommandLine.Option(names = {"-2", "--ms2"}, description = "MS2 spectra files", required = true, paramLabel = "<ms2File>[,<ms2File>...]", order = 332)
        protected void setMs2(String ms2Files){
            this.ms2 = Arrays.stream(ms2Files.split(",")).map(File::new).collect(Collectors.toList());
        }
        public List<File> ms2;

        @CommandLine.Option(names = {"-z", "--parentmass", "--precursor", "--mz"}, description = "The mass of the parent ion for the specified ms2 spectra", required = true, order = 333)
        public Double parentMz;

        @CommandLine.Option(names = {"--ionization", "--adduct"}, description = "Specify the adduct for this compound", defaultValue = "[M+?]+", showDefaultValue = CommandLine.Help.Visibility.ALWAYS, order = 334)
        protected void setIonType(String ionType) {
            this.ionType = PrecursorIonType.fromString(ionType);
        }

        public PrecursorIonType ionType;

        @CommandLine.Option(names = {"-f", "--formula"}, description = "Specify the neutralized formula of this compound. This will be used for tree computation. If given no mass decomposition will be performed.", order = 335)
        public void setFormula(String formula) {
            this.formula = MolecularFormula.parseOrThrow(formula);
        }

        public MolecularFormula formula = null;
    }
}
