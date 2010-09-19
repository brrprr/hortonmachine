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
package org.jgrasstools.hortonmachine.externals.epanet.core;

import java.util.HashMap;

@SuppressWarnings("nls")
public class EpanetErrors {
    private static HashMap<Integer, String> errors = new HashMap<Integer, String>();
    static {
        errors.put(0, "No error ");
        errors.put(101, "Insufficient memory ");
        errors.put(102, "No network data to process ");
        errors.put(103, "Hydraulics solver not initialized ");
        errors.put(104, "No hydraulic results available ");
        errors.put(105, "Water quality solver not initialized ");
        errors.put(106, "No results to report on ");
        errors.put(110, "Cannot solve hydraulic equations ");
        errors.put(120, "Cannot solve WQ transport equations ");
        errors.put(200, "One or more errors in input file ");
        errors.put(202, "Illegal numeric value in function call ");
        errors.put(203, "Undefined node in function call ");
        errors.put(204, "Undefined link in function call ");
        errors.put(205, "Undefined time pattern in function call ");
        errors.put(207, "Attempt made to control a check valve ");
        errors.put(223, "Not enough nodes in network ");
        errors.put(224, "No tanks or reservoirs in network ");
        errors.put(240, "Undefined source in function call ");
        errors.put(241, "Undefined control statement in function call ");
        errors.put(250, "Function argument has invalid format ");
        errors.put(251, "Illegal parameter code in function call ");
        errors.put(301, "Identical file names ");
        errors.put(302, "Cannot open input file ");
        errors.put(303, "Cannot open report file ");
        errors.put(304, "Cannot open binary output file ");
        errors.put(305, "Cannot open hydraulics file ");
        errors.put(306, "Invalid hydraulics file ");
        errors.put(307, "Cannot read hydraulics file ");
        errors.put(308, "Cannot save results to file ");
        errors.put(309, "Cannot write report to file ");
    }

    public static void checkError( int errcode ) throws EpanetException {
        if (errcode > 0) {
            String msg = errors.get(errcode);
            throw new EpanetException(msg);
        }
    }
    


}
