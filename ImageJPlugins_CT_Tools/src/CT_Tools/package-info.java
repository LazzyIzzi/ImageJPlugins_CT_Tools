/**
 * 
CT_Tools is a suite of plugins for routing X-ray calculations
Calculators<br>
Linear Attenuation - Calculates MuLin using formula, density, and keV<br>
Spectrum Plotter - Plots MuMass NIST data for a formula vs keV<br>
Lookup MuLin - returns keV(s) given a MuLin, formula, density<br>
Lookup Ratio - returns keV(s) given a pair of  MuLin, formula, density<br>
Workflow* (sequence of steps for model-based linearization of a beam hardened CT slice)<br>
1. Scanner Setup - Optimise scan time, beam-hardening for a given source, filter, and detector<br>
2. Material Tagger** - builds a \"TagImage\" model by segmenting the CT slice into known components<br>
3. Tag Image To Parallel Brems Sinogram - CT scans the model using source, filter, and detector settings<br>
4. Reconstruct - uses DFI to reconstruct the output of step 3<br>
5. Linearization Fitter - finds beam-hardening solutions by comparing the image to its model reconstruction at selected energies<br>
6. Apply Linearization - corrects the image sinogram using the selected solution<br>
7. Reconstruct Linearized - reconstructs the corrected sinogram<br>
*CT slices must be in attenuation units of CM-1 and pixel size in CM<br>
**For severely beam-hardened CT slices, re-project the CT slice using Projectors->\"MuLin Image To Parallel_Sinogram\"<br>
 followed by DFI JTransform's BH corrector to suppress the cupping artifact<br>
Tools
Materials Editor - edits the list of material tags,names,formulas and densities used for attenuation calculations<br>
Materials Editor Excel - edits the materials list using Excel of other .csv file editor<br>
Tag Image To Fan Brems Sinogram - Fan-beam CT scans the model using source, filter, and detector settings<br>
Tag Image To Parallel Brems Sinogram - same as Workflow item 3<br>
Tag Image To MuLin - converts a tag image the linear attenuation at the selected keV<br>
Attenuation Error - creates a difference image from the model slice and corrected slice at the selected keV<br>
Attenuation To Effective Energy - computes an image of the effective energy of each component in the corrected slice "<br>
Reconstructors<br>
DFI JTransforms - reconstructs parallel beam sinograms using direct Fourier inversion";<br>
		
DFI_JTransforms_Beta is an ImageJ plugin for reconstructing parallel-beam<br>
(0-180deg) sinograms into tomographic slices using Direct Fourier Inversion<br>
as described by Flannery et al. Science 18 Sep 1987 Vol 237, Issue 4821 pp.<br>
1439-1444 DOI: 10.1126/science.237.4821.1439<br>
Download JTransforms-3.1-with-dependencies.jar to the plugins folder for the<br>
forward and inverse FTs<br>
 * @see <a href=
 *      "https://javadoc.io/doc/com.github.wendykierp/JTransforms/latest/index.html">JTransforms</a>
 * @author LazzyIzzi
 *
 */
package CT_Tools;
