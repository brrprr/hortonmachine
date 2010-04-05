/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package eu.hydrologis.jgrass.hortonmachine.models.hm;

import java.io.IOException;
import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.hydrologis.jgrass.hortonmachine.libs.monitor.PrintStreamProgressMonitor;
import eu.hydrologis.jgrass.hortonmachine.modules.geomorphology.curvatures.Curvatures;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestCase;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestMaps;
import eu.hydrologis.jgrass.hortonmachine.utils.coverage.CoverageUtilities;
/**
 * It test the {@link Curvatures} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestCurvatures extends HMTestCase {

    public void testCurvatures() throws IOException {
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;

        double[][] pitfillerData = HMTestMaps.pitData;
        GridCoverage2D pitfillerCoverage = CoverageUtilities.buildCoverage("pitfiller",
                pitfillerData, envelopeParams, crs);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);

        Curvatures curvatures = new Curvatures();
        curvatures.inDem = pitfillerCoverage;
        curvatures.pm = pm;

        curvatures.process();

        GridCoverage2D profCoverage = curvatures.outProf;
        GridCoverage2D planCoverage = curvatures.outPlan;
        GridCoverage2D tangCoverage = curvatures.outTang;

        checkMatrixEqual(profCoverage.getRenderedImage(), HMTestMaps.profData, 0.0001);
        checkMatrixEqual(planCoverage.getRenderedImage(), HMTestMaps.planData, 0.0001);
        checkMatrixEqual(tangCoverage.getRenderedImage(), HMTestMaps.tanData, 0.0001);
    }
}
