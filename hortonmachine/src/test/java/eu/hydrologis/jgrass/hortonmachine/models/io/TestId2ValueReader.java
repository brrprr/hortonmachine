package eu.hydrologis.jgrass.hortonmachine.models.io;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

import eu.hydrologis.jgrass.hortonmachine.io.timedependent.TimeseriesByStepReaderId2Value;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestCase;
/**
 * Test Id2ValueReader.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestId2ValueReader extends HMTestCase {

    public void testId2ValueReader() throws Exception {
        URL krigingRainUrl = this.getClass().getClassLoader().getResource("kriging_rain.csv");

        TimeseriesByStepReaderId2Value reader = new TimeseriesByStepReaderId2Value();
        reader.file = new File(krigingRainUrl.toURI()).getAbsolutePath();
        reader.idfield = "ID";

        reader.nextRecord();
        HashMap<Integer, double[]> id2ValueMap = reader.data;
        assertEquals(1.74, id2ValueMap.get(1)[0]);
        assertEquals(1.34, id2ValueMap.get(2)[0]);
        assertEquals(1.61, id2ValueMap.get(3)[0]);
        assertEquals(2.15, id2ValueMap.get(4)[0]);
        assertEquals(1.57, id2ValueMap.get(5)[0]);
        assertEquals(1.15, id2ValueMap.get(6)[0]);

        reader.nextRecord();
        id2ValueMap = reader.data;
        assertEquals(1.71, id2ValueMap.get(1)[0]);
        assertEquals(1.37, id2ValueMap.get(2)[0]);
        assertEquals(1.62, id2ValueMap.get(3)[0]);
        assertEquals(2.18, id2ValueMap.get(4)[0]);
        assertEquals(1.63, id2ValueMap.get(5)[0]);
        assertEquals(1.19, id2ValueMap.get(6)[0]);
        
        reader.close();
    }
}
