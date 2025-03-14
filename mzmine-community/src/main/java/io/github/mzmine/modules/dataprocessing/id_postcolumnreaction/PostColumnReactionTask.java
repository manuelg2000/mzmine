package io.github.mzmine.modules.dataprocessing.id_postcolumnreaction;

import static io.github.mzmine.modules.dataprocessing.id_formulapredictionfeaturelist.FormulaPredictionFeatureListParameters.elements;

import io.github.mzmine.datamodel.MZmineProject;
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
import io.github.mzmine.modules.dataprocessing.id_formulapredictionfeaturelist.FormulaPredictionFeatureListParameters;
import io.github.mzmine.modules.dataprocessing.id_formulapredictionfeaturelist.FormulaPredictionFeatureListTask;
import io.github.mzmine.modules.dataprocessing.id_online_reactivity.OnlineLcReactivityModule;
import io.github.mzmine.modules.dataprocessing.id_online_reactivity.OnlineLcReactivityTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  private Map<String, Integer> annotationCounts = new HashMap<>();

  private final int totalRows;

  private MZmineProject project;
  private ParameterSet parameters;
  private RawDataFilesSelection unreactedSelection;
  private List<RawDataFile> unreactedRaws;

  public PostColumnReactionTask(@NotNull ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate, parameters, OnlineLcReactivityModule.class);

    this.parameters = parameters;
    this.unreactedSelection = parameters.getParameter(
        PostColumnReactionParameters.unreactedRawDataFiles).getValue();
    this.unreactedRaws = List.of(unreactedSelection.getMatchingRawDataFiles().clone());
    this.flist = parameters.getParameter(
            PostColumnReactionParameters.flist).getValue()
        .getMatchingFeatureLists()[0];
    totalRows = flist.getNumberOfRows();

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
    List<FeatureListRow> annotatedRows = rows.stream().filter(FeatureListRow::isIdentified).toList();

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
        FeatureListRow correlatedRow = rowsRelationship.getOtherRow(annotatedRow);

        // Check if the feature is present in any unreacted raw files
        boolean isInUnreacted = unreactedRaws.stream()
            .anyMatch(unreactedRaw -> correlatedRow.hasFeature(unreactedRaw));

        if (!isInUnreacted) {
          // Annotate unannotated features
          annotateUnannotatedFeature(correlatedRow, annotatedRow);
        }
      });
    }

    setStatus(TaskStatus.FINISHED);
  }

  private void annotateUnannotatedFeature(FeatureListRow correlatedRow, FeatureListRow baseRow) {
    if (correlatedRow.getPreferredAnnotation() == null || correlatedRow.getCompoundAnnotations().isEmpty()) {
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
      }
    }
  }

  public static void predictCorrelatedFormula(FeatureListRow correlatedRow, FeatureListRow baseRow) {

    try {
      FormulaPredictionFeatureListParameters params = new FormulaPredictionFeatureListParameters();
      MolecularFormulaRange molecularFormulaRange = new MolecularFormulaRange();
      List<CompoundDBAnnotation> baseRowCompoundAnnotations = baseRow.getCompoundAnnotations();
      String baseFomrulaString = baseRowCompoundAnnotations.getFirst().getFormula();
      IMolecularFormula baseFormula = MolecularFormulaManipulator.getMolecularFormula(baseFomrulaString, DefaultChemObjectBuilder.getInstance());

      Iterable<IIsotope> isotopes = baseFormula.isotopes();
      List<FeatureListRow> correlatedRows = new ArrayList<>();  // this is not yet very elegant. The task creates a feature list with one row for each prediction because the prediction task uses a feature list as input. Maybe create one feature list for all correlated rows of one annotation and then run the formula prediction. Alternatively, the prediction task can be adjusted to accept one single feature list row.
      correlatedRows.add(correlatedRow);
      IsotopeFactory iFac = Isotopes.getInstance();

      for (IIsotope i : isotopes) {
        IIsotope majorIsotope = iFac.getMajorIsotope(i.getSymbol());
        int baseIsotopeCount = baseFormula.getIsotopeCount(i);
        int isotopeCount = baseIsotopeCount;
        if (i.getSymbol() == "O" || i.getSymbol() == "H") {
            isotopeCount = baseIsotopeCount + 8;
        }
        molecularFormulaRange.addIsotope(majorIsotope, 0, isotopeCount);
      }

      ParameterSet parameters = params.cloneParameterSet();
      parameters.getParameter(elements).setValue(molecularFormulaRange);

      FormulaPredictionFeatureListTask newTask =
          new FormulaPredictionFeatureListTask(null, correlatedRows, parameters, Instant.now());
      newTask.run();
    }
    catch (Exception e) {
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
