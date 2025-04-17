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

import static io.github.mzmine.modules.dataprocessing.id_formulapredictionfeaturelist.FormulaPredictionFeatureListParameters.elementalRatios;
import static io.github.mzmine.modules.dataprocessing.id_formulapredictionfeaturelist.FormulaPredictionFeatureListParameters.elements;
import static io.github.mzmine.modules.dataprocessing.id_formulapredictionfeaturelist.FormulaPredictionFeatureListParameters.ionization;
import static io.github.mzmine.modules.dataprocessing.id_formulapredictionfeaturelist.FormulaPredictionFeatureListParameters.isotopeFilter;
import static io.github.mzmine.modules.dataprocessing.id_formulapredictionfeaturelist.FormulaPredictionFeatureListParameters.msmsFilter;
import static io.github.mzmine.modules.dataprocessing.id_formulapredictionfeaturelist.FormulaPredictionFeatureListParameters.mzTolerance;
import static io.github.mzmine.modules.dataprocessing.id_formulapredictionfeaturelist.FormulaPredictionFeatureListParameters.rdbeRestrictions;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionParameters;
import io.github.mzmine.modules.dataprocessing.id_formulapredictionfeaturelist.FormulaPredictionFeatureListParameters;
import io.github.mzmine.modules.dataprocessing.id_formulapredictionfeaturelist.FormulaPredictionFeatureListTask;
import io.github.mzmine.modules.dataprocessing.id_online_reactivity.OnlineLcReactivityModule;
import io.github.mzmine.modules.dataprocessing.id_online_reactivity.OnlineLcReactivityTask;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreParameters;
import io.github.mzmine.modules.tools.msmsscore.MSMSScoreParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class PostColumnReactionTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(OnlineLcReactivityTask.class.getName());

  private final FeatureList flist;
  private final String description;
  private final Map<String, Integer> annotationCounts = new HashMap<>();
  private final List<RawDataFile> unreactedRaws;
  private final ParameterSet predParamSet;
  private final double corrThreshold;


  public PostColumnReactionTask(@NotNull ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate, parameters, OnlineLcReactivityModule.class);

    corrThreshold = parameters.getParameter(PostColumnReactionParameters.correlationThreshold)
        .getValue();

    RawDataFilesSelection unreactedSelection = parameters.getParameter(
        PostColumnReactionParameters.unreactedRawDataFiles).getValue();
    IonizationType ionType = parameters.getParameter(PostColumnReactionParameters.ionization)
        .getValue();
    MZTolerance predMzTolerance = parameters.getParameter(PostColumnReactionParameters.mzTolerance)
        .getValue();
    boolean checkHeurParams = parameters.getParameter(PostColumnReactionParameters.elementalRatios)
        .getValue();
    ElementalHeuristicParameters heurParams = parameters.getParameter(
        PostColumnReactionParameters.elementalRatios).getEmbeddedParameters();
    boolean checkRDBEParams = parameters.getParameter(PostColumnReactionParameters.rdbeRestrictions)
        .getValue();
    RDBERestrictionParameters rdbeParams = parameters.getParameter(
        PostColumnReactionParameters.rdbeRestrictions).getEmbeddedParameters();
    boolean checkIsotopeParams = parameters.getParameter(PostColumnReactionParameters.isotopeFilter)
        .getValue();
    IsotopePatternScoreParameters isotopeParams = parameters.getParameter(
        PostColumnReactionParameters.isotopeFilter).getEmbeddedParameters();
    boolean checkMSMSParams = parameters.getParameter(PostColumnReactionParameters.msmsFilter)
        .getValue();
    MSMSScoreParameters msmsParams = parameters.getParameter(
        PostColumnReactionParameters.msmsFilter).getEmbeddedParameters();
    this.unreactedRaws = List.of(unreactedSelection.getMatchingRawDataFiles().clone());
    this.flist = parameters.getParameter(PostColumnReactionParameters.flist).getValue()
        .getMatchingFeatureLists()[0];
    FormulaPredictionFeatureListParameters predParams = new FormulaPredictionFeatureListParameters();
    this.predParamSet = predParams.cloneParameterSet();
    this.predParamSet.getParameter(ionization).setValue(ionType);
    this.predParamSet.getParameter(mzTolerance).setValue(predMzTolerance);

    //heurParams
    if (checkHeurParams) {
      this.predParamSet.getParameter(elementalRatios).setValue(true);
      this.predParamSet.getParameter(elementalRatios).setEmbeddedParameters(heurParams);
    } else {
      this.predParamSet.getParameter(elementalRatios).setValue(false);
    }

    //rdbeParams
    if (checkRDBEParams) {
      this.predParamSet.getParameter(rdbeRestrictions).setValue(true);
      this.predParamSet.getParameter(rdbeRestrictions).setEmbeddedParameters(rdbeParams);
    } else {
      this.predParamSet.getParameter(rdbeRestrictions).setValue(false);
    }

    //isotopeParams
    if (checkIsotopeParams) {
      this.predParamSet.getParameter(isotopeFilter).setValue(true);
      this.predParamSet.getParameter(isotopeFilter).setEmbeddedParameters(isotopeParams);
    } else {
      this.predParamSet.getParameter(isotopeFilter).setValue(false);
    }

    //msmsParams
    if (checkMSMSParams) {
      this.predParamSet.getParameter(msmsFilter).setValue(true);
      this.predParamSet.getParameter(msmsFilter).setEmbeddedParameters(msmsParams);
    } else {
      this.predParamSet.getParameter(msmsFilter).setValue(false);
    }

    setStatus(TaskStatus.WAITING);
    logger.setLevel(Level.FINEST);

    description = "Annotate online reaction products on " + flist.getName();
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(flist);
  }

  @Override
  protected void process() {
    setStatus(TaskStatus.PROCESSING);

    if (!checkUnreactedSelection(flist, unreactedRaws)) {
      setErrorMessage("Feature list " + flist.getName()
          + " does no contain all selected unreacted raw data files.");
      setStatus(TaskStatus.ERROR);
      return;
    }

    // get the files that are considered as reacted
    final List<RawDataFile> reactedRaws = new ArrayList<>();
    for (RawDataFile file : flist.getRawDataFiles()) {
      if (!unreactedRaws.contains(file)) {
        reactedRaws.add(file);
      }
    }

    logger.finest(() -> flist.getName() + " contains " + reactedRaws.size()
        + " raw data files not classified as reacted.");

    List<FeatureListRow> rows = flist.getRows().stream().sorted(FeatureListRowSorter.MZ_ASCENDING)
        .toList();
    if (rows.isEmpty()) {
      logger.info("Empty feature list " + flist.getName());
      setStatus(TaskStatus.FINISHED);
      return;
    }

    // Filter rows with annotations
    List<FeatureListRow> annotatedRows = rows.stream().filter(FeatureListRow::isIdentified)
        .toList();

    if (annotatedRows.isEmpty()) {
      logger.info("No annotated rows in feature list " + flist.getName());
      setStatus(TaskStatus.FINISHED);
      return;
    }

    R2RMap<RowsRelationship> correlationMap = flist.getMs1CorrelationMap().orElse(null);
    if (correlationMap == null || correlationMap.isEmpty()) {
      MZmineCore.getDesktop()
          .displayMessage("Run correlation grouping before running this module " + flist.getName());
      setStatus(TaskStatus.FINISHED);
      return;
    }

    // Process correlated rows for each annotated row
    for (FeatureListRow annotatedRow : annotatedRows) {
      correlationMap.streamAllCorrelatedRows(annotatedRow, rows).forEach(rowsRelationship -> {
        if (rowsRelationship.getScore() >= corrThreshold) {
          FeatureListRow correlatedRow = rowsRelationship.getOtherRow(annotatedRow);

          // Check if the feature is present in any unreacted raw files
          boolean isInUnreacted;
          isInUnreacted = unreactedRaws.stream().anyMatch(correlatedRow::hasFeature);

          if (!isInUnreacted) {
            // Annotate unannotated features
            annotateUnannotatedFeature(correlatedRow, annotatedRow);
          }
        }
      });
    }

    setStatus(TaskStatus.FINISHED);
  }

  private void annotateUnannotatedFeature(FeatureListRow correlatedRow, FeatureListRow baseRow) {
    if (correlatedRow.getPreferredAnnotation() == null || correlatedRow.getCompoundAnnotations()
        .isEmpty()) {
//      Optional<FeatureAnnotation> annotationWithFormula = CompoundAnnotationUtils.streamFeatureAnnotations(baseRow)
//          .filter(a -> StringUtils.hasValue(a.getFormula())).findFirst();
//
//      if(annotationWithFormula.isPresent()) {
//        FeatureAnnotation annotation = annotationWithFormula.get();
//      }

      String baseAnnotation = baseRow.getPreferredAnnotationName();
      if (baseAnnotation != null) {
        String roundedMz = String.valueOf(Math.round(correlatedRow.getAverageMZ()));
        String baseTpAnnotation = baseAnnotation + "_ETP_" + roundedMz;

        // Get the current count for this base annotation
        int count = annotationCounts.getOrDefault(baseTpAnnotation, 0);
        String tpAnnotation;

        if (count > 0) {
          char suffix = (char) ('a' + count);
          tpAnnotation = baseTpAnnotation + suffix;
        } else {
          tpAnnotation = baseTpAnnotation;
        }

        // Increment the count for this base annotation
        annotationCounts.put(baseTpAnnotation, count + 1);

        SimpleCompoundDBAnnotation annotation = new SimpleCompoundDBAnnotation();
        annotation.put(PrecursorMZType.class, correlatedRow.getAverageMZ());
        annotation.put(CompoundNameType.class, tpAnnotation);
        correlatedRow.addCompoundAnnotation(annotation);

        predictCorrelatedFormula(correlatedRow, baseRow);
        if (correlatedRow.getFormulas() != null && !correlatedRow.getFormulas().isEmpty()) {
          ResultFormula correlatedFormula = correlatedRow.getFormulas().getFirst();
          annotation.setFormula(correlatedFormula.toString());
        }
      }
    }
  }

  public void predictCorrelatedFormula(FeatureListRow correlatedRow, FeatureListRow baseRow) {

    try {
      MolecularFormulaRange molecularFormulaRange = new MolecularFormulaRange();
      List<CompoundDBAnnotation> baseRowCompoundAnnotations = baseRow.getCompoundAnnotations();
      String baseFomrulaString;
      baseFomrulaString = baseRowCompoundAnnotations.getFirst().getFormula();
      assert baseFomrulaString != null;
      IMolecularFormula baseFormula = MolecularFormulaManipulator.getMolecularFormula(
          baseFomrulaString, DefaultChemObjectBuilder.getInstance());

      Iterable<IIsotope> isotopes = baseFormula.isotopes();
      List<FeatureListRow> correlatedRows = new ArrayList<>();  // this is not yet very elegant. The task creates a feature list with one row for each prediction because the prediction task uses a feature list as input. Maybe create one feature list for all correlated rows of one annotation and then run the formula prediction. Alternatively, the prediction task can be adjusted to accept one single feature list row.
      correlatedRows.add(correlatedRow);
      IsotopeFactory iFac = Isotopes.getInstance();
      IIsotope oxygenIsotope = iFac.getMajorIsotope("O");

      for (IIsotope i : isotopes) {
        IIsotope majorIsotope = iFac.getMajorIsotope(i.getSymbol());
        int baseIsotopeCount = baseFormula.getIsotopeCount(i);
        int isotopeCount = baseIsotopeCount;
        if (Objects.equals(i.getSymbol(), "O") || Objects.equals(i.getSymbol(), "H")) {
          isotopeCount = baseIsotopeCount + 8;
        }
        molecularFormulaRange.addIsotope(majorIsotope, 0, isotopeCount);
      }

      if (molecularFormulaRange.contains(oxygenIsotope)) {
        this.predParamSet.getParameter(elements).setValue(molecularFormulaRange);
      } else {
        molecularFormulaRange.addIsotope(oxygenIsotope, 0, 8);
        this.predParamSet.getParameter(elements).setValue(molecularFormulaRange);
      }

      FormulaPredictionFeatureListTask newTask = new FormulaPredictionFeatureListTask(null,
          correlatedRows, this.predParamSet, Instant.now());
      newTask.run();
    } catch (Exception e) {
      logger.severe("Error predicting molecular formula: " + e.getMessage());
    }
  }

  private boolean checkUnreactedSelection(FeatureList aligned, List<RawDataFile> unreactedRaws) {

    List<RawDataFile> flRaws = aligned.getRawDataFiles();

    for (int i = 0; i < unreactedRaws.size(); i++) {
      boolean contained = false;

      for (RawDataFile flRaw : flRaws) {
        if (unreactedRaws.get(i) == flRaw) {
          contained = true;
          break;
        }
      }

      if (!contained) {
        final int i1 = i;
        logger.info(() -> "Feature list " + aligned.getName() + " does not contain raw data files "
            + unreactedRaws.get(i1).getName());
        return false;
      }
    }

    logger.finest(
        () -> "Feature list " + aligned.getName() + " contains all selected blank raw data files.");
    return true;
  }

}
