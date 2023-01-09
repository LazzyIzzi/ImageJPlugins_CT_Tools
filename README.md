# ImageJPlugins_CT_Tools
ImageJ UI examples calling the MuMassCalculator library.

The plugins in this folder create a virtual microCT scanner. A full description can be found <a href="https://lazzyizzi.github.io/" target="_blank">here</a>.

<ul>
<li>There are calculators for computing attenuation coefficients.</li>
<li>Finding the X-ray energy from attenuation coefficients and coefficient ratios.</li>
<li>Displaying mass attenuation spectra of compounds.</li>
<li>Examining the hardening of an conventional X-ray intensity distribution as it passes from source to detector.</li>
<li>CT scanning images and image stacks with fan-beam or parallel-beam monochromatic and conventional X-ray sources. </li>
<li>Reconstructing parallel-beam sinograms.</li>
<li>Estimating optimal polynomial coefficients for correcting beam-hardening artifact.</li>
</ul>

Caveats: The virtual scanners do not model noise.  The detector in the virtual scanner reports absorbed counts based only on the detector material and thickness. Real detectors are much more complicated.

Dependencies:  MuMassCalculatorJava8Lib, GenericDialogAddins.

ImageJ_CT_ToolsBundle_Java8.jar contains all you need to create a virtual CT lab.

