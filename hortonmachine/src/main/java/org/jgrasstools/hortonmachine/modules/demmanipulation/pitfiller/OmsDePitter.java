/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jgrasstools.hortonmachine.modules.demmanipulation.pitfiller;

import static org.jgrasstools.gears.libs.modules.JGTConstants.DEMMANIPULATION;

import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.GridNode;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.multiprocessing.GridMultiProcessing;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.BitMatrix;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

@Description(OmsDePitter.OMSDEPITTER_DESCRIPTION)
@Author(name = OmsDePitter.OMSDEPITTER_AUTHORNAMES, contact = OmsDePitter.OMSDEPITTER_AUTHORCONTACTS)
@Keywords(OmsDePitter.OMSDEPITTER_KEYWORDS)
@Label(OmsDePitter.OMSDEPITTER_LABEL)
@Name(OmsDePitter.OMSDEPITTER_NAME)
@Status(OmsDePitter.OMSDEPITTER_STATUS)
@License(OmsDePitter.OMSDEPITTER_LICENSE)
public class OmsDePitter extends GridMultiProcessing {
    @Description(OMSDEPITTER_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev;

    @Description(OMSDEPITTER_outPit_DESCRIPTION)
    @Out
    public GridCoverage2D outPit = null;

    @Description(OMSDEPITTER_outFlow_DESCRIPTION)
    @Out
    public GridCoverage2D outFlow = null;

    // @Description(OMSDEPITTER_outPitPoints_DESCRIPTION)
    // @Out
    // public SimpleFeatureCollection outPitPoints = null;

    public static final String OMSDEPITTER_DESCRIPTION = "The module fills the depression points present within a DEM and generates a map of flowdirections that also handles flat areas.";
    public static final String OMSDEPITTER_DOCUMENTATION = "";
    public static final String OMSDEPITTER_KEYWORDS = "Dem manipulation, Geomorphology";
    public static final String OMSDEPITTER_LABEL = DEMMANIPULATION;
    public static final String OMSDEPITTER_NAME = "depit";
    public static final int OMSDEPITTER_STATUS = Status.EXPERIMENTAL;
    public static final String OMSDEPITTER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSDEPITTER_AUTHORNAMES = "Andrea Antonello, Silvia Franceschi";
    public static final String OMSDEPITTER_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSDEPITTER_inElev_DESCRIPTION = "The map of digital elevation model (DEM).";
    public static final String OMSDEPITTER_outPit_DESCRIPTION = "The depitted elevation map.";
    public static final String OMSDEPITTER_outPitPoints_DESCRIPTION = "The shapefile of handled pits.";
    public static final String OMSDEPITTER_outFlow_DESCRIPTION = "The map of D8 flowdirections.";

    private final float delta = 2E-6f;
    private boolean verbose = true;

    private int cols;

    private int rows;

    private double xRes;

    private double yRes;

    @Execute
    public void process() throws Exception {
        checkNull(inElev);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        cols = regionMap.getCols();
        rows = regionMap.getRows();
        xRes = regionMap.getXres();
        yRes = regionMap.getYres();

        // output raster
        WritableRaster pitRaster = CoverageUtilities.renderedImage2DoubleWritableRaster(inElev.getRenderedImage(), false);
        WritableRandomIter pitIter = CoverageUtilities.getWritableRandomIterator(pitRaster);
        try {

            ConcurrentLinkedQueue<GridNode> pitsList = getPitsList(cols, rows, xRes, yRes, pitIter);
            BitMatrix allPitsPositions = new BitMatrix(cols, rows);

            int count = 0;
            int iteration = 1;
            while( pitsList.size() > 0 ) {// || flatsList.size() > 0 ) {
                if (pm.isCanceled()) {
                    return;
                }

                int pitCount = pitsList.size();

                List<GridNode> processedNodesInPit = new ArrayList<>();

                int shownCount = pitCount;
                if (!verbose) {
                    shownCount = IJGTProgressMonitor.UNKNOWN;
                }
                pm.beginTask("Processing " + pitCount + " pits (iteration N." + iteration++ + ")... ", shownCount);
                for( GridNode originalPitNode : pitsList ) {
                    if (allPitsPositions.isMarked(originalPitNode.col, originalPitNode.row)) {
                        if (verbose)
                            pm.worked(1);
                        continue;
                    }
                    count++;
                    if (pm.isCanceled()) {
                        return;
                    }

                    List<GridNode> nodesInPit = new ArrayList<>();
                    nodesInPit.add(originalPitNode);
                    allPitsPositions.mark(originalPitNode.col, originalPitNode.row);

                    double minExitValue = Double.POSITIVE_INFINITY;
                    GridNode minExitValueNode = null;
                    int workingIndex = 0;
                    while( workingIndex < nodesInPit.size() ) {
                        if (pm.isCanceled()) {
                            return;
                        }
                        GridNode currentPitNode = nodesInPit.get(workingIndex);

                        List<GridNode> surroundingNodes = new ArrayList<>(currentPitNode.getValidSurroundingNodes());
                        removeExistingPits(allPitsPositions, surroundingNodes);
                        GridNode minEqualNode = getMinEqualElevNode(surroundingNodes, allPitsPositions);
                        if (minEqualNode == null) {
                            workingIndex++;
                            continue;
                        }

                        List<GridNode> minElevSurroundingNodes = new ArrayList<>(minEqualNode.getValidSurroundingNodes());
                        removeExistingPits(allPitsPositions, minElevSurroundingNodes);
                        if (!minEqualNode.isPitFor(minElevSurroundingNodes)) {
                            /*
                             * case of a pit that is solved by the nearby cell
                             */
                            if (minEqualNode != null && minEqualNode.elevation < minExitValue) {
                                minExitValue = minEqualNode.elevation;
                                minExitValueNode = minEqualNode;
                            }
                            break;
                        }

                        /*
                         * can't find a non pit exit in the nearest surrounding, therefore 
                         * we need to have a look at all the surrounding and have a look 
                         * of any of those cells are able to flow out somewhere, i.e. are not pit 
                         */
                        for( GridNode tmpNode : surroundingNodes ) {
                            if (tmpNode.touchesBound() || !tmpNode.isValid()) {
                                continue;
                            }
                            List<GridNode> subSurroundingNodes = new ArrayList<>(tmpNode.getValidSurroundingNodes());
                            removeExistingPits(allPitsPositions, subSurroundingNodes);

                            if (tmpNode.isPitFor(subSurroundingNodes)) {
                                nodesInPit.add(tmpNode);
                                allPitsPositions.mark(tmpNode.col, tmpNode.row);

                                // GridNode subMinNode = getMinEqualElevNode(subSurroundingNodes,
                                // allPitsPositions);
                                //
                                // if (subMinNode != null && subMinNode.elevation < minExitValue) {
                                // minExitValue = subMinNode.elevation;
                                // minExitValueNode = subMinNode;
                                // }
                            }
                        }
                        workingIndex++;
                    }

                    if (Double.isInfinite(minExitValue)) {
                        for( GridNode gridNode : nodesInPit ) {
                            if (gridNode.elevation > minExitValue) {
                                minExitValue = gridNode.elevation;
                                minExitValueNode = gridNode;
                            }
                        }
                    }

                    if (Double.isInfinite(minExitValue) || Double.isNaN(minExitValue)) {
                        throw new RuntimeException("Found invalid value at: " + count);
                    }

                    // allPitsPositions.mark(minExitValueNode.col, minExitValueNode.row);
                    // // add connected that have same value of the future flooded pit
                    // findConnectedOfSameHeight(minExitValueNode, allPitsPositions, nodesInPit);

                    PitInfo info = new PitInfo();
                    // info.originalPitNode = originalPitNode;
                    info.pitfillExitNode = minExitValueNode;
                    info.nodes = nodesInPit;
                    processedNodesInPit.addAll(nodesInPit);

                    GridNode pitfillExitNode = info.pitfillExitNode;
                    List<GridNode> allPitsOfCurrent = info.nodes;

                    BitMatrix floodedPositions = new BitMatrix(cols, rows);
                    floodAndFlow(0, pitfillExitNode.col, pitfillExitNode.row, pitfillExitNode.elevation, allPitsOfCurrent,
                            floodedPositions, pitIter);

                    // List<GridNode> updated = new ArrayList<>();
                    // for( GridNode gridNode : nodesInPit ) {
                    // GridNode updatedNode = new GridNode(pitIter, gridNode.cols, gridNode.rows,
                    // gridNode.xRes, gridNode.yRes, gridNode.col,
                    // gridNode.row);
                    // updated.add(updatedNode);
                    // }
                    // nodesInPit = updated;

                    if (verbose)
                        pm.worked(1);
                }
                pm.done();

                pitsList = getPitsList(cols, rows, xRes, yRes, pitIter, allPitsPositions);

                int size = pitsList.size();
                pm.message("Left pits: " + size);
                pm.message("---------------------------------------------------------------------");
                if (size < 10000) {
                    verbose = false;
                }

            }

            outPit = CoverageUtilities.buildCoverage("pitfiller", pitRaster, regionMap, inElev.getCoordinateReferenceSystem());
            // GridGeometry2D gridGeometry = inElev.getGridGeometry();
            //
            // SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            // b.setName("pitpoints");
            // b.setCRS(inElev.getCoordinateReferenceSystem());
            // b.add("the_geom", Point.class);
            // b.add("type", String.class);
            // b.add("origelev", Double.class);
            // b.add("newelev", Double.class);
            // SimpleFeatureType type = b.buildFeatureType();
            // SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
            //
            // outPitPoints = new DefaultFeatureCollection();
            //
            // pm.beginTask("Generating pitpoints...", nRows);
            // for( int r = 0; r < nRows; r++ ) {
            // if (pm.isCanceled()) {
            // return;
            // }
            // for( int c = 0; c < nCols; c++ ) {
            // if (allPitsPositions.isMarked(c, r)) {
            // Coordinate coordinate = CoverageUtilities.coordinateFromColRow(c, r, gridGeometry);
            // Point point = gf.createPoint(coordinate);
            // double origValue = CoverageUtilities.getValue(inElev, c, r);
            // double newValue = pitIter.getSampleDouble(c, r, 0);
            //
            // Object[] values = new Object[]{point, "pit", origValue, newValue};
            // builder.addAll(values);
            // SimpleFeature feature = builder.buildFeature(null);
            // ((DefaultFeatureCollection) outPitPoints).add(feature);
            // } else if (allFlatsPositions.isMarked(c, r)) {
            // Coordinate coordinate = CoverageUtilities.coordinateFromColRow(c, r, gridGeometry);
            // Point point = gf.createPoint(coordinate);
            // double origValue = CoverageUtilities.getValue(inElev, c, r);
            // double newValue = pitIter.getSampleDouble(c, r, 0);
            //
            // Object[] values = new Object[]{point, "flat", origValue, newValue};
            // builder.addAll(values);
            // SimpleFeature feature = builder.buildFeature(null);
            // ((DefaultFeatureCollection) outPitPoints).add(feature);
            // }
            // }
            // }

            WritableRaster flowRaster = CoverageUtilities.createWritableRaster(cols, rows, Short.class, null, null);
            WritableRandomIter flowIter = CoverageUtilities.getWritableRandomIterator(flowRaster);
            try {
                pm.beginTask("Calculating flowdirections...", rows * cols);
                processGrid(cols, rows, false, ( c, r ) -> {
                    if (pm.isCanceled()) {
                        return;
                    }

                    GridNode node = new GridNode(pitIter, cols, rows, xRes, yRes, c, r);
                    boolean isValid = node.isValid();
                    if (!isValid || node.touchesBound() || node.touchesNovalue()) {
                        flowIter.setSample(c, r, 0, JGTConstants.intNovalue);
                    } else {
                        GridNode nextDown = node.goDownstreamSP();
                        if (nextDown == null) {// || nextDown.touchesNovalue() ||
                                               // nextDown.touchesBound()) {
                            flowIter.setSample(c, r, 0, JGTConstants.intNovalue);
                            // flowIter.setSample(c, r, 0, FlowNode.OUTLET);
                        } else {
                            int newFlow = node.getFlow();
                            flowIter.setSample(c, r, 0, newFlow);
                        }
                    }
                    pm.worked(1);
                });
                pm.done();

                outFlow = CoverageUtilities.buildCoverage("flow", flowRaster, regionMap, inElev.getCoordinateReferenceSystem());
            } finally {
                flowIter.done();
            }

        } finally {
            pitIter.done();
        }
    }

    private void removeExistingPits( BitMatrix allPitsPositions, List<GridNode> surroundingNodes ) {
        Iterator<GridNode> nodesIter = surroundingNodes.iterator();
        while( nodesIter.hasNext() ) {
            GridNode gridNode = (GridNode) nodesIter.next();
            if (allPitsPositions.isMarked(gridNode.col, gridNode.row)) {
                nodesIter.remove();
            }
        }
    }

    private GridNode getMinEqualElevNode( List<GridNode> surroundingNodes, BitMatrix allPitsPositions ) {
        double minElev = Double.POSITIVE_INFINITY;
        GridNode minNode = null;
        for( GridNode gridNode : surroundingNodes ) {
            if (gridNode.elevation <= minElev && !allPitsPositions.isMarked(gridNode.col, gridNode.row)) {
                minElev = gridNode.elevation;
                minNode = gridNode;
            }
        }
        return minNode;
    }

    private void floodAndFlow( int iteration, int runningCol, int runnningRow, double runningFloodValueValue,
            List<GridNode> allNodesOfPit, BitMatrix floodedPositions, WritableRandomIter pitIter ) {
        iteration++;

        GridNode currentNode = new GridNode(pitIter, cols, rows, xRes, yRes, runningCol, runnningRow);

        // TODO make first surrounding and then recurse on sub-surrounding ordering the cells from
        // the middle

        // flood and grow the first surrounding
        List<GridNode> validSurroundingNodes = currentNode.getValidSurroundingNodes();
        List<GridNode> toUseNodes = new ArrayList<>();
        for( GridNode gridNode : validSurroundingNodes ) {
            if (!floodedPositions.isMarked(gridNode.col, gridNode.row) && allNodesOfPit.contains(gridNode)) {
                double newValue = 0;
                if (gridNode.elevation <= runningFloodValueValue) {
                    newValue = runningFloodValueValue + delta;
                } else {
                    newValue = gridNode.elevation; // TODO check if it is enough or better add delta
                }
                gridNode.setValueInMap(pitIter, newValue);
                floodedPositions.mark(gridNode.col, gridNode.row);
                toUseNodes.add(gridNode);
            }
        }

        int size = toUseNodes.size();
        if (size > 1) {
            int half = size / 2;
            for( int i = half; i >= 0; i-- ) {
                GridNode gridNode = toUseNodes.get(i);
                floodAndFlow(iteration, gridNode.col, gridNode.row, gridNode.elevation, allNodesOfPit, floodedPositions, pitIter);
            }
            for( int i = half + 1; i < size; i++ ) {
                GridNode gridNode = toUseNodes.get(i);
                floodAndFlow(iteration, gridNode.col, gridNode.row, gridNode.elevation, allNodesOfPit, floodedPositions, pitIter);
            }
        } else if (size == 1) {
            GridNode gridNode = toUseNodes.get(0);
            floodAndFlow(iteration, gridNode.col, gridNode.row, gridNode.elevation, allNodesOfPit, floodedPositions, pitIter);
        }
        // List<GridNode> collectedSubSurroundingNodes = new ArrayList<>();
        // for( GridNode gridNode : validSurroundingNodes ) {
        // List<GridNode> subSurroundingNodes = gridNode.getValidSurroundingNodes();
        // for( GridNode subGridNode : subSurroundingNodes ) {
        // if (!floodedPositions.isMarked(subGridNode.col, subGridNode.row) &&
        // allNodesOfPit.contains(subGridNode)) {
        // collectedSubSurroundingNodes.add(subGridNode);
        // }
        // }
        // }
        //
        // int size = collectedSubSurroundingNodes.size();
        // if (size > 1) {
        // int half = size / 2;
        // for( int i = half; i >= 0; i-- ) {
        // GridNode subGridNode = collectedSubSurroundingNodes.get(i);
        // floodAndFlow(iteration, subGridNode.col, subGridNode.row, subGridNode.elevation,
        // allNodesOfPit, floodedPositions,
        // pitIter);
        // }
        // for( int i = half + 1; i < size; i++ ) {
        // GridNode subGridNode = collectedSubSurroundingNodes.get(i);
        // floodAndFlow(iteration, subGridNode.col, subGridNode.row, subGridNode.elevation,
        // allNodesOfPit, floodedPositions,
        // pitIter);
        // }
        // } else if (size == 1) {
        // GridNode gridNode = collectedSubSurroundingNodes.get(0);
        // floodAndFlow(iteration, gridNode.col, gridNode.row, gridNode.elevation, allNodesOfPit,
        // floodedPositions, pitIter);
        // }

    }

    /**
     * Make cells flow ready by creating a slope starting from the output cell.
     * 
     * @param iteration the iteration.
     * @param pitfillExitNode the exit node.
     * @param cellsToMakeFlowReady the cells to check and change at each iteration.
     * @param allPitsPositions the marked positions of all existing pits. Necessary to pick only those that 
     *              really are part of the pit pool.
     * @param pitIter elevation data.
     * @param delta the elevation delta to add to the cells to create the slope.
     */
    private void makeCellsFlowReady( int iteration, GridNode pitfillExitNode, List<GridNode> cellsToMakeFlowReady,
            BitMatrix allPitsPositions, WritableRandomIter pitIter, float delta ) {
        iteration++;

        double exitElevation = pitfillExitNode.elevation;
        List<GridNode> connected = new ArrayList<>();
        for( GridNode checkNode : cellsToMakeFlowReady ) {
            List<GridNode> validSurroundingNodes = checkNode.getValidSurroundingNodes();
            for( GridNode gridNode : validSurroundingNodes ) {
                if (!pitfillExitNode.equals(gridNode) && allPitsPositions.isMarked(gridNode.col, gridNode.row)
                        && gridNode.elevation == exitElevation) {
                    if (!connected.contains(gridNode))
                        connected.add(gridNode);
                }
            }
        }
        if (connected.size() == 0) {
            return;
        }

        for( GridNode gridNode : connected ) {
            double newElev = (double) (gridNode.elevation + delta * (double) iteration);
            gridNode.setValueInMap(pitIter, newElev);
        }
        List<GridNode> updatedConnected = new ArrayList<>();
        for( GridNode gridNode : connected ) {
            GridNode updatedNode = new GridNode(pitIter, gridNode.cols, gridNode.rows, gridNode.xRes, gridNode.yRes, gridNode.col,
                    gridNode.row);
            updatedConnected.add(updatedNode);
        }
        makeCellsFlowReady(iteration, pitfillExitNode, updatedConnected, allPitsPositions, pitIter, delta);
    }

    private ConcurrentLinkedQueue<GridNode> getPitsList( int nCols, int nRows, double xRes, double yRes,
            WritableRandomIter pitIter ) {
        ConcurrentLinkedQueue<GridNode> pitsList = new ConcurrentLinkedQueue<>();
        if (verbose)
            pm.beginTask("Extract pits from DTM...", nRows);
        for( int row = 0; row < nRows; row++ ) {
            for( int col = 0; col < nCols; col++ ) {
                GridNode node = new GridNode(pitIter, nCols, nRows, xRes, yRes, col, row);
                if (node.isPit()) {
                    double surroundingMin = node.getSurroundingMin();
                    if (Double.isInfinite(surroundingMin)) {
                        continue;
                    }
                    pitsList.add(node);
                }
            }
            if (verbose)
                pm.worked(1);
        }
        if (verbose)
            pm.done();
        return pitsList;
    }

    private ConcurrentLinkedQueue<GridNode> getPitsList( int nCols, int nRows, double xRes, double yRes,
            WritableRandomIter pitIter, BitMatrix allPitsPositions ) {
        ConcurrentLinkedQueue<GridNode> pitsList = new ConcurrentLinkedQueue<>();

        BitMatrix currentMarked = new BitMatrix(nCols, nRows);
        if (verbose)
            pm.beginTask("Extract pits from DTM...", nRows);
        for( int row = 0; row < nRows; row++ ) {
            for( int col = 0; col < nCols; col++ ) {
                if (!allPitsPositions.isMarked(col, row)) {
                    continue;
                }

                GridNode node = new GridNode(pitIter, nCols, nRows, xRes, yRes, col, row);
                if (node.isPit()) {
                    double surroundingMin = node.getSurroundingMin();
                    if (Double.isInfinite(surroundingMin)) {
                        continue;
                    }
                    pitsList.add(node);
                }

                // check also border ones
                List<GridNode> validSurroundingNodes = node.getValidSurroundingNodes();
                for( GridNode gridNode : validSurroundingNodes ) {
                    if (allPitsPositions.isMarked(gridNode.col, gridNode.row)
                            || currentMarked.isMarked(gridNode.col, gridNode.row)) {
                        // we want only border
                        continue;
                    }
                    if (gridNode.isPit()) {
                        double surroundingMin = gridNode.getSurroundingMin();
                        if (Double.isInfinite(surroundingMin)) {
                            continue;
                        }
                        pitsList.add(gridNode);
                        currentMarked.mark(gridNode.col, gridNode.row);
                    }
                }

            }
            if (verbose)
                pm.worked(1);
        }
        if (verbose)
            pm.done();
        return pitsList;
    }

    private void findConnectedOfSameHeight( GridNode node, BitMatrix allPitsPositions, List<GridNode> nodesInPit ) {
        double elev = node.elevation;
        List<GridNode> checkNodes = new ArrayList<>();
        checkNodes.add(node);
        boolean oneAdded = true;
        int startIndex = 0;
        while( oneAdded ) {
            oneAdded = false;
            int currentSize = checkNodes.size();
            for( int i = startIndex; i < currentSize; i++ ) {
                GridNode checkNode = checkNodes.get(i);
                List<GridNode> tmpNodes = checkNode.getValidSurroundingNodes();
                for( GridNode gridNode : tmpNodes ) {
                    if (allPitsPositions.isMarked(gridNode.col, gridNode.row)) {
                        continue;
                    }

                    if (gridNode.elevation == elev) {
                        checkNodes.add(gridNode);
                        nodesInPit.add(gridNode);
                        allPitsPositions.mark(gridNode.col, gridNode.row);
                        oneAdded = true;
                    }
                }
            }
            startIndex = currentSize;
        }

    }

    private static class PitInfo {
        // GridNode originalPitNode;
        GridNode pitfillExitNode;
        List<GridNode> nodes = new ArrayList<>();

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Exit node: ").append(pitfillExitNode).append("\n");
            sb.append("Number of connected nodes: " + nodes.size() + "\n");
            for( GridNode gridNode : nodes ) {
                sb.append("  -> col=" + gridNode.col + " row=" + gridNode.row + " elev=" + gridNode.elevation + "\n");
            }
            return sb.toString();
        }
    }

}
