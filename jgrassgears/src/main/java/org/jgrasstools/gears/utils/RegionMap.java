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
package org.jgrasstools.gears.utils;

import java.util.HashMap;

import org.jgrasstools.gears.libs.modules.JGTConstants;

import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.*;

/**
 * Map containing a region definition, having utility methods to get the values.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class RegionMap extends HashMap<String, Double> {
    private static final long serialVersionUID = 1L;

    /**
     * Getter for the region cols.
     * 
     * @return the region cols or -1.
     */
    public int getCols() {
        Double cols = get(COLS);
        if (cols != null) {
            return cols.intValue();
        }
        return -1;
    }

    /**
     * Getter for the region rows.
     * 
     * @return the region rows or -1.
     */
    public int getRows() {
        Double rows = get(ROWS);
        if (rows != null) {
            return rows.intValue();
        }
        return -1;
    }

    /**
     * Getter for the region's north bound.
     * 
     * @return the region north bound or {@link JGTConstants#doubleNovalue}
     */
    public double getNorth() {
        Double n = get(NORTH);
        if (n != null) {
            return n;
        }
        return JGTConstants.doubleNovalue;
    }

    /**
     * Getter for the region's south bound.
     * 
     * @return the region south bound or {@link JGTConstants#doubleNovalue}
     */
    public double getSouth() {
        Double s = get(SOUTH);
        if (s != null) {
            return s;
        }
        return JGTConstants.doubleNovalue;
    }

    /**
     * Getter for the region's east bound.
     * 
     * @return the region east bound or {@link JGTConstants#doubleNovalue}
     */
    public double getEast() {
        Double e = get(EAST);
        if (e != null) {
            return e;
        }
        return JGTConstants.doubleNovalue;
    }

    /**
     * Getter for the region's west bound.
     * 
     * @return the region west bound or {@link JGTConstants#doubleNovalue}
     */
    public double getWest() {
        Double w = get(WEST);
        if (w != null) {
            return w;
        }
        return JGTConstants.doubleNovalue;
    }

    /**
     * Getter for the region's X resolution.
     * 
     * @return the region's X resolution or {@link JGTConstants#doubleNovalue}
     */
    public double getXres() {
        Double xres = get(XRES);
        if (xres != null) {
            return xres;
        }
        return JGTConstants.doubleNovalue;
    }

    /**
     * Getter for the region's Y resolution.
     * 
     * @return the region's Y resolution or {@link JGTConstants#doubleNovalue}
     */
    public double getYres() {
        Double yres = get(YRES);
        if (yres != null) {
            return yres;
        }
        return JGTConstants.doubleNovalue;
    }
}