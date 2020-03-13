package de.unijena.bioinf.ms.gui.io.spectrum;


import de.unijena.bioinf.ms.gui.ms_viewer.data.MolecularFormulaInformation;
import de.unijena.bioinf.ms.gui.ms_viewer.data.PeakInformation;

import java.util.Collections;
import java.util.List;

public class SiriusMSViewerPeak implements PeakInformation {
	
	private double absInt, relInt, mass, sn;

	public SiriusMSViewerPeak() {
		absInt = 0;
		relInt = 0;
		mass = 0;
		sn = 0;
	}
	
	

	public void setAbsoluteIntensity(double absInt) {
		this.absInt = absInt;
	}



	public void setRelativeIntensity(double relInt) {
		this.relInt = relInt;
	}



	public void setMass(double mass) {
		this.mass = mass;
	}



	public void setSn(double sn) {
		this.sn = sn;
	}
	
	@Override
	public double getAbsoluteIntensity() {
		return absInt;
	}

	@Override
	public List<MolecularFormulaInformation> getDecompositions() {
		return Collections.EMPTY_LIST;
	}

	@Override
	public double getMass() {
		return mass;
	}

	@Override
	public double getRelativeIntensity() {
		return relInt;
	}

	@Override
	public boolean isIsotope() {
		return false;
	}
	
}