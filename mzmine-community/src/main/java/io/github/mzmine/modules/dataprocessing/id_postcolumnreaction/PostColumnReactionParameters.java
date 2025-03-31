package io.github.mzmine.modules.dataprocessing.id_postcolumnreaction;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionParameters;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreParameters;
import io.github.mzmine.modules.tools.msmsscore.MSMSScoreParameters;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class PostColumnReactionParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flist = new FeatureListsParameter(
      "Aligned feature list", 1, 1);

  public static final RawDataFilesParameter unreactedRawDataFiles = new RawDataFilesParameter(
      "Unreacted raw data files", 1, Integer.MAX_VALUE  );

  public static final ComboParameter<IonizationType> ionization = new ComboParameter<>(
      "Ionization type", "Ionization type", IonizationType.values(),
      IonizationType.POSITIVE_HYDROGEN);

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(0.002, 2);

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


  public PostColumnReactionParameters() {
    super(flist, unreactedRawDataFiles, ionization, mzTolerance, elementalRatios, rdbeRestrictions, isotopeFilter, msmsFilter);
  }
}
