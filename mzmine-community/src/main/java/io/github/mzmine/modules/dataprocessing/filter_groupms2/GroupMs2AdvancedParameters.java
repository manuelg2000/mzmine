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

package io.github.mzmine.modules.dataprocessing.filter_groupms2;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import org.jetbrains.annotations.Nullable;

public class GroupMs2AdvancedParameters extends SimpleParameterSet {

  public static final OptionalParameter<DoubleParameter> outputNoiseLevel = new OptionalParameter<>(
      new DoubleParameter("Minimum signal intensity (absolute, TIMS)",
          "If a TIMS feature is processed, this parameter "
              + "can be used to filter low abundant signals in the MS/MS spectrum, since multiple "
              + "MS/MS mobility scans need to be merged together.",
          MZmineCore.getConfiguration().getIntensityFormat(), 250d, 0d, Double.MAX_VALUE), false);

  public static final OptionalParameter<PercentParameter> outputNoiseLevelRelative = new OptionalParameter<>(
      new PercentParameter("Minimum signal intensity (relative, TIMS)",
          "If an ion mobility spectrometry (TIMS) feature is processed, this parameter "
              + "can be used to filter low abundant peaks in the MS/MS spectrum, since multiple "
              + "MS/MS mobility scans need to be merged together.", 0.01d), true);

  public GroupMs2AdvancedParameters() {
    super(outputNoiseLevel, outputNoiseLevelRelative);
  }

  public static GroupMs2AdvancedParameters create(@Nullable Double outputNoiseLevel,
      @Nullable Double outputNoiseLevelRelative) {
    final GroupMs2AdvancedParameters param = (GroupMs2AdvancedParameters) new GroupMs2AdvancedParameters().cloneParameterSet();

    param.setParameter(GroupMs2AdvancedParameters.outputNoiseLevel, outputNoiseLevel != null,
        outputNoiseLevel);
    param.setParameter(GroupMs2AdvancedParameters.outputNoiseLevelRelative,
        outputNoiseLevelRelative != null, outputNoiseLevelRelative);

    return param;
  }
}
