/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.Warning;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.jjobs.JobProgressMerger;
import de.unijena.bioinf.jjobs.ProgressInputStream;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.validation.Ms1Validator;
import de.unijena.bioinf.sirius.validation.Ms2Validator;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * File based input Iterator that allows to iterate over the {@see de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment}s parsed from
 * multiple files (also different types) that are supported by the {@see de.unijena.bioinf.babelms.MsExperimentParser}.
 */
public class MS2ExpInputIterator implements InstIterProvider {
    private static final Logger LOG = LoggerFactory.getLogger(MS2ExpInputIterator.class);
    private final ArrayDeque<Ms2Experiment> instances = new ArrayDeque<>();
    private final Iterator<Path> fileIter;
    private final Predicate<Ms2Experiment> filter;
    private final MsExperimentParser parser = new MsExperimentParser();
    private final boolean ignoreFormula;
    private final boolean allowMS1Only;

    @Nullable
    private final JobProgressMerger progress;

    Path currentFile;
    Iterator<Ms2Experiment> currentExperimentIterator;

    public MS2ExpInputIterator(Collection<Path> input, double maxMz, boolean ignoreFormula, boolean allowMS1Only) {
        this(input, (exp) -> exp.getIonMass() <= maxMz, ignoreFormula, allowMS1Only, null);
    }

    public MS2ExpInputIterator(Collection<Path> input, Predicate<Ms2Experiment> filter, boolean ignoreFormula, boolean allowMS1Only, @Nullable JobProgressMerger progress) {
        this.progress = progress;
        this.fileIter = input.iterator();
        this.filter = filter;
        this.ignoreFormula = ignoreFormula;
        this.allowMS1Only = allowMS1Only;
        currentExperimentIterator = fetchNext();
    }

    @Override
    public boolean hasNext() {
        return !instances.isEmpty();
    }

    @Override
    public Ms2Experiment next() {
        fetchNext();
        return instances.poll();
    }

    private Iterator<Ms2Experiment> fetchNext() {
        start:
        while (true) {
            if (currentExperimentIterator == null || !currentExperimentIterator.hasNext()) {
                if (fileIter.hasNext()) {
                    currentFile = fileIter.next();
                    try {
                        GenericParser<Ms2Experiment> p = parser.getParser(currentFile);
                        if (p == null) {
                            LOG.error("Unknown file format: '" + currentFile + "'");
                        } else {
                            if (progress == null) {
                                currentExperimentIterator = p.parseFromPathIterator(currentFile);
                            } else {
                                ProgressInputStream s = new ProgressInputStream(currentFile);
                                s.addPropertyChangeListener(progress);
                                currentExperimentIterator = p.parseIterator(s, currentFile.toUri());
                            }
                        }
                    } catch (Exception e) {
                        LOG.error("Cannot parse file '" + currentFile + "':\n", e);
                    }
                } else return null;
            } else {
                try {
                    MutableMs2Experiment experiment = Sirius.makeMutable(currentExperimentIterator.next());

                    if (experiment.getPrecursorIonType() == null) {
                        LOG.warn("No ion or charge given for: " + experiment.getName() + " Try guessing charge from name.");
                        final String name = (Optional.ofNullable(experiment.getName()).orElse("") +
                                "_" + Optional.ofNullable(experiment.getSourceString()).orElse("")).toLowerCase();

                        if ((name.contains("negative") || name.contains("neg")) && (!name.contains("positive") && !name.contains("pos"))) {
                            LOG.info(experiment.getName() + ": Negative charge keyword found!");
                            experiment.setPrecursorIonType(PrecursorIonType.unknownNegative());
                        } else {
                            LOG.info(experiment.getName() + ": Falling back to positive");
                            experiment.setPrecursorIonType(PrecursorIonType.unknownPositive());
                        }
                    }

                    if (experiment.getMs1Spectra().removeIf(Spectrum::isEmpty))
                        LoggerFactory.getLogger(getClass()).warn("Removed at lease one empty MS1 spectrum from '" + experiment.getName() + "'.");
                    if (experiment.getMs2Spectra().removeIf(Spectrum::isEmpty))
                        LoggerFactory.getLogger(getClass()).warn("Removed at lease one empty MS/MS spectrum from '" + experiment.getName() + "'.");

                    if (!allowMS1Only && experiment.getMs2Spectra().isEmpty()) {
                        LOG.info("Skipping instance '" + experiment.getName() + "' because it does not contain any non Empty MS/MS.");
                    } else if (!filter.test(experiment)) {
                        LOG.info("Skipping instance '" + experiment.getName() + "' because it did not pass the filter setting.");
                    } else if (experiment.getMolecularFormula() != null && experiment.getMolecularFormula().numberOf("D") > 0) {
                        LOG.warn("Deuterium Formula found in: " + experiment.getName() + " Instance will be Ignored.");
                    } else {
                        if (ignoreFormula) {
                            experiment.setMolecularFormula(null);
                            experiment.removeAnnotation(InChI.class);
                            experiment.removeAnnotation(Smiles.class);
                        }
                        if (experiment.getMs2Spectra().isEmpty()){
                            new Ms1Validator().validate(experiment, Warning.Logger, true);
                        } else {
                            new Ms2Validator().validate(experiment, Warning.Logger, true);
                        }
                        instances.add(experiment);
                        return currentExperimentIterator;
                    }
                } catch (Exception e) {
                    LOG.error("Error while parsing compound! Skipping entry", e);
                }
            }
        }
    }
}