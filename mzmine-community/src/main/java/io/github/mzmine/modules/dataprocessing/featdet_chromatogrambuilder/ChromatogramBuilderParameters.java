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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogrambuilder;

import static io.github.mzmine.javafx.components.factories.FxTexts.boldText;
import static io.github.mzmine.javafx.components.factories.FxTexts.text;

import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.ExitCode;
import javafx.scene.layout.Region;

public class ChromatogramBuilderParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
      new ScanSelection(1));

  public static final DoubleParameter minimumTimeSpan = new DoubleParameter("Min time span (min)",
      "Minimum time span over which the same ion must be observed in order to be recognized as a chromatogram.\n"
          + "The optimal value depends on the chromatography system setup. The best way to set this parameter\n"
          + "is by studying the raw data and determining what is the typical time span of chromatographic peaks.",
      MZmineCore.getConfiguration().getRTFormat());

  public static final DoubleParameter minimumHeight = new DoubleParameter("Min height",
      "Minimum intensity of the highest data point in the chromatogram. If chromatogram height is below this level, it is discarded.",
      MZmineCore.getConfiguration().getIntensityFormat());

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final StringParameter suffix = new StringParameter("Suffix",
      "This string is added to filename as suffix", "chromatograms");

  public ChromatogramBuilderParameters() {
    super(new Parameter[]{dataFiles, scanSelection, minimumTimeSpan, minimumHeight, mzTolerance,
        suffix});
  }

  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    Region message = FxTextFlows.newTextFlowInAccordion("Important note", true, boldText("Note:\n"),
        text("""
            starting with MZmine 2.39, this module is considered deprecated
            and will be removed in future MZmine versions. Please use the ADAP Chromatogram Builder
            which is much faster and generates better results.
            Contact the developers if you have any questions or concerns.
            """));

    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);
    dialog.showAndWait();
    return dialog.getExitCode();

  }

}
