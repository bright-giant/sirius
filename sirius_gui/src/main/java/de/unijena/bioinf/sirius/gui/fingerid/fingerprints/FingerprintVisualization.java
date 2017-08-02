package de.unijena.bioinf.sirius.gui.fingerid.fingerprints;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.chemdb.ChemicalDatabase;
import de.unijena.bioinf.chemdb.DatabaseException;
import de.unijena.bioinf.fingerid.Fingerprinter;
import de.unijena.bioinf.fingerid.fingerprints.ECFPFingerprinter;
import gnu.trove.list.array.TIntArrayList;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.CircularFingerprinter;
import org.openscience.cdk.fingerprint.IBitFingerprint;
import org.openscience.cdk.graph.matrix.AdjacencyMatrix;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.smiles.smarts.SMARTSQueryTool;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

class FingerprintVisualization {

    public static void main(String[] args) throws IOException {
        final PrintStream stream = new PrintStream("feature_examples.tsv");
        final CdkFingerprintVersion version = CdkFingerprintVersion.getComplete();
        final List<String> lines = Files.readAllLines(new File("summary.csv").toPath(), Charset.forName("UTF-8"));
        final int[] atomSizes = new int[version.size()];
        Arrays.fill(atomSizes, -1);
        try (final ChemicalDatabase db = new ChemicalDatabase()) {
            final SMARTSQueryTool query = new SMARTSQueryTool("C-C", SilentChemObjectBuilder.getInstance());
            query.setQueryCacheSize(atomSizes.length);
            final ECFPFingerprinter fingerprinter = new ECFPFingerprinter();
            final int ECFP_OFFSET = version.getOffsetFor(CdkFingerprintVersion.USED_FINGERPRINTS.ECFP);
            final String[] substructures = new String[fingerprinter.getSize()];
            final String[] smarts = new String[fingerprinter.getSize()];
            final List<String> compounds = Files.readAllLines(new File("compounds.csv").toPath(), Charset.forName("UTF-8"));
            final InChIGeneratorFactory igf = InChIGeneratorFactory.getInstance();
            for (String aline : compounds) {
                final String[] linetabs = aline.split("\t");
                final String inchi = linetabs[2];
                final String inchikey = linetabs[1];
                final IAtomContainer molecule = igf.getInChIToStructure(inchi, SilentChemObjectBuilder.getInstance()).getAtomContainer();
                final int[][] matrix = AdjacencyMatrix.getMatrix(molecule);
                final IBitFingerprint fp = fingerprinter.getBitFingerprint(molecule);
                final CircularFingerprinter.FP[] details = fingerprinter.getFingerprintDetails();
                int k=0;
                for (int i=0; i < fp.size(); ++i) {
                    if (fp.get(i)) {
                        boolean aroma=false;
                        final CircularFingerprinter.FP x = details[i];
                        final TIntArrayList inds = new TIntArrayList();
                        final BitSet ids = new BitSet(molecule.getAtomCount());
                        for (int atomi : x.atoms) {
                            if (ids.get(atomi)) continue;
                            final IAtom atom = molecule.getAtom(atomi);
                            aroma = aroma || atom.isAromatic();
                            inds.add(atomi);
                            ids.set(atomi);
                            addAromaticRings(molecule, matrix, atomi, atom, ids, inds);
                        }
                        if (substructures[i]==null || !aroma) {
                            final String smiles = SmilesGenerator.absolute().aromatic().create(AtomContainerManipulator.extractSubstructure(molecule, inds.toArray()));
                            substructures[i] = smiles;
                            atomSizes[i+ECFP_OFFSET] = x.atoms.length;
                            addNeighbours(molecule, matrix, ids, inds);
                            final String smart = SmilesGenerator.absolute().aromatic().create(AtomContainerManipulator.extractSubstructure(molecule, inds.toArray()));
                            smarts[i] = smart;
                        }
                    }
                }

                Fingerprint fingerprint = db.lookupFingerprintByInChI(new InChI(inchikey, inchi));
                if (fingerprint==null) {
                    // compute it...
                    final Fingerprinter fingerprinter1 = Fingerprinter.getFor(CdkFingerprintVersion.getComplete());
                    fingerprint = new BooleanFingerprint(CdkFingerprintVersion.getComplete(), fingerprinter1.fingerprintsToBooleans(fingerprinter1.computeFingerprints(molecule)));
                }
                if (fingerprint != null) {

                    for (FPIter f : fingerprint) {
                        if (f.isSet() && atomSizes[f.getIndex()]<0) {
                            if (f.getMolecularProperty() instanceof SubstructureProperty) {

                                query.setSmarts(((SubstructureProperty) f.getMolecularProperty()).getSmarts());
                                if (query.matches(molecule)) {
                                    final List<List<Integer>> size = query.getUniqueMatchingAtoms();
                                    if (!size.isEmpty()) {
                                        atomSizes[f.getIndex()] = Math.max(atomSizes[f.getIndex()], size.get(0).size());
                                    }
                                }
                            }
                        }
                    }
                }

            }

            final CdkFingerprintVersion v = CdkFingerprintVersion.getComplete();
            final SMARTSQueryTool queryTool = new SMARTSQueryTool("C#C", SilentChemObjectBuilder.getInstance());
            queryTool.setQueryCacheSize(6000);
            for (String line : lines) {
                String[] tabs = line.split("\t");
                final MolecularProperty prop = v.getMolecularProperty(Integer.parseInt(tabs[0]));
                stream.print(tabs[0]);
                stream.print("\t");
                stream.print(atomSizes[Integer.parseInt(tabs[0])]);

                if (tabs.length==2) {
                    stream.print("\t" + tabs[1]);
                } else {
                    for (int i=2; i < tabs.length; i += 2) {

                        final String substruct = tabs[i];
                        try {
                            final IAtomContainer substructure = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(substruct);
                            if (prop instanceof SubstructureProperty) {
                                queryTool.setSmarts(((SubstructureProperty) prop).getSmarts());
                                if (queryTool.matches(substructure)) {
                                    List<List<Integer>> match = queryTool.getUniqueMatchingAtoms();
                                    if (match.size()>0 && match.get(0).size()>0) {
                                        stream.print("\t" + tabs[i]);
                                    } else {
                                        System.err.println("WTF? Match but no atoms. For " + tabs[0] + " with " + prop.getDescription() + " and substructure " + substruct);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                stream.print("\n");
            }

            final int offset = version.getOffsetFor(CdkFingerprintVersion.USED_FINGERPRINTS.ECFP);
            for (int i=0; i < substructures.length; ++i) {
                if (substructures[i]==null) {
                    System.err.println("Warning: No smiles for ECFP with index "+ i +  " and hash " + version.getMolecularProperty(offset+i).getDescription());
                } else {
                    stream.print(offset+i);
                    stream.print("\t");
                    stream.print(atomSizes[i+offset]);
                    stream.print("\t");
                    stream.print(substructures[i]);
                    stream.print("\t");
                    stream.println(smarts[i]);
                }
            }

        } catch (CDKException e) {
            e.printStackTrace();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        } catch (DatabaseException e) {
            e.printStackTrace();
        }
        stream.close();
        // now add ECFP fingerprints

    }

    private static void addNeighbours(IAtomContainer molecule, int[][] matrix, BitSet ids, TIntArrayList inds) {
        for (int i : inds.toArray()) {
            for (int j=0; j < matrix.length; ++j) {
                if (matrix[i][j]>0 && !ids.get(j)) {
                    inds.add(j);
                    ids.set(j);
                }
            }
        }
    }

    private static void addAromaticRings(IAtomContainer molecule, int[][] matrix, int atomId, IAtom atom, BitSet ids, TIntArrayList inds) {
        if (!atom.isAromatic()) return;
        // find the smallest aromatic ring
        final BitSet visited = new BitSet(molecule.getAtomCount());
        final TIntArrayList stack = new TIntArrayList(molecule.getAtomCount());
        stack.add(atomId);
        while (!stack.isEmpty()) {
            final int id = stack.removeAt(stack.size()-1);
            for (int i=0; i < matrix.length; ++i) {
                if (matrix[id][i]>0 && i != id) {
                    if (!visited.get(i) && molecule.getAtom(i).isAromatic() && molecule.getAtom(i).isAromatic()) {
                        visited.set(i);
                        stack.add(i);
                        if (!ids.get(i)) inds.add(i);
                    }
                }
            }
        }
    }

    protected String[] exampleSmiles;
    protected int numberOfMatchesAtoms;

    protected static FingerprintVisualization[] read() throws IOException {
        final CdkFingerprintVersion cdk = CdkFingerprintVersion.getComplete();
        final FingerprintVisualization[] viz = new FingerprintVisualization[cdk.size()];
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(FingerprintVisualization.class.getResourceAsStream("/sirius/feature_examples.tsv")))) {
            String line;
            while ((line=br.readLine())!=null) {
                String[] tabs = line.split("\t");
                final int index = Integer.parseInt(tabs[0]);
                viz[index] = new FingerprintVisualization(tabs);
            }
        }
        return viz;
    }

    private FingerprintVisualization(String... tabs) {
        final CdkFingerprintVersion cdk = CdkFingerprintVersion.getComplete();
        exampleSmiles = new String[(tabs.length-2)];
        int k=0;
        for (int i=2; i < tabs.length; ++i) {
            exampleSmiles[k++] = tabs[i];
        }
        numberOfMatchesAtoms = Integer.parseInt(tabs[1]);
    }

    public int getNumberOfExamples() {
        return exampleSmiles.length;
    }

    public String getExample(int num) {
        return exampleSmiles[num];
    }

    public int getMatchSize() {
        return numberOfMatchesAtoms;
    }

}
