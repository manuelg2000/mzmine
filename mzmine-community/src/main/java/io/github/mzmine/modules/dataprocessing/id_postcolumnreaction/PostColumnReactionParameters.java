/*
 * Copyright (c) 2004-2025 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_postcolumnreaction;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionParameters;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreParameters;
import io.github.mzmine.modules.tools.msmsscore.MSMSScoreParameters;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class PostColumnReactionParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flist = new FeatureListsParameter(
      "Aligned feature list", 1, 1);

  public static final RawDataFilesParameter unreactedRawDataFiles = new RawDataFilesParameter(
      "Unreacted raw data files", 1, Integer.MAX_VALUE);

  public static final ComboParameter<IonizationType> ionization = new ComboParameter<>(
      "Ionization type", "Ionization type", IonizationType.values(),
      IonizationType.POSITIVE_HYDROGEN);

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(0.001, 2);

  public static final OptionalModuleParameter<ElementalHeuristicParameters> elementalRatios = new OptionalModuleParameter<>(
      "Element count heuristics",
      "Restrict formulas by heuristic restrictions of elemental counts and ratios",
      new ElementalHeuristicParameters(), false);

  public static final OptionalModuleParameter<RDBERestrictionParameters> rdbeRestrictions = new OptionalModuleParameter<>(
      "RDBE restrictions",
      "Search only for formulas which correspond to the given RDBE restrictions",
      new RDBERestrictionParameters(), false);

  public static final OptionalModuleParameter<IsotopePatternScoreParameters> isotopeFilter = new OptionalModuleParameter<>(
      "Isotope pattern filter", "Search only for formulas with a isotope pattern similar",
      new IsotopePatternScoreParameters(), false);

  public static final OptionalModuleParameter<MSMSScoreParameters> msmsFilter = new OptionalModuleParameter<>(
      "MS/MS filter", "Check MS/MS data", new MSMSScoreParameters(), false);

  public static final PercentParameter correlationThreshold = new PercentParameter(
      "Correlation threshold",
      "Correlation score at which features are considered for transformation product annotation in %.",
      0.4);


  public PostColumnReactionParameters() {
    super(flist, unreactedRawDataFiles, ionization, mzTolerance, elementalRatios, rdbeRestrictions,
        isotopeFilter, msmsFilter, correlationThreshold);
  }
}
