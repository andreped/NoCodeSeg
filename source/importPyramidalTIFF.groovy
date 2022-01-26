 /**
 * Script to import uint8 pyramidal TIFF images as annotations for QuPath >= v0.3*
 *
 * It is assumed that the TIFF lies in a user-defined directory with the same name but with the extension '.tiff'
 * 
 * Furthermore, we assume that the image is uint8, where each uint correspond to a different class
 *
 * Currently assumed that for k classes of interest, they are labelled {1, 2, 3, ..., k}, with no space between
 *
 * Code is slightly modified from Peter Bankhead's script to support RunForProject:
 * https://gist.github.com/petebankhead/27c1f8cd950583452c756f3a2ea41fc0
 *
 * which was written for https://forum.image.sc/t/rendering-wsi-as-overlay-on-top-of-another-wsi/52629/25?u=andreped
 *
 * All credit to Peter Bankhead and the QuPath team for their tremendous support in implementing this script!
 *
 * @author André Pedersen
 */


// --- SET THESE PARAMETERS ---
def masksPath = "C:/Users/andrp/workspace/FP_projects/multiclass_test/results/"  // path to where TIFFs are stored
def downsample = 4                      // which scaling factor to use, if the segmentation is produced at a lower magnification level
def level = 0                           // which level to extract segmentation from (choosing 0 may be slow)
def extension = ".tiff"                 // pyramidal TIFF
def classNames = ["Benign", "Malign"]   // names of classes of interest
int channel = 0                         // 0-based index for the channel to threshold
boolean fromFP = true                   // if TIFF is created in FastPathology, should set this to 'true'
// ----------------------------


// Get a list of image files, stopping early if none can be found
def dirOutput = new File(masksPath);
if (!dirOutput.isDirectory()) {
    print dirOutput + ' is not a valid directory!';
    return;
}

// get current WSI, update paths to file
def currWSIName = GeneralTools.getNameWithoutExtension(getProjectEntry().getImageName())
if (fromFP) {
    masksPath += currWSIName
}
def path = masksPath + "/" + currWSIName + extension

// check if file exists, if no return
File file = new File(path)
if (!file.exists()) {
    print path + ' does not exist!';
    return;
}

// Create a single-resolution server at the desired level, if required
def server = buildServer(path)
if (level != 0) {
    server = qupath.lib.images.servers.ImageServers.pyramidalize(server, server.getDownsampleForResolution(level))
}

def belowClass = getPathClass('Ignore*')     // Class for pixels below the threshold
def current_thresh = 0.5;

classNames.each { currClassName ->
    def aboveClass = getPathClass(currClassName)     // Class for pixels above the threshold
    
    // Create a thresholded image
    def thresholdServer = PixelClassifierTools.createThresholdServer(server, channel, current_thresh, belowClass, getPathClass(currClassName))
    
    // Create annotations and add to the current object hierarchy
    def hierarchy = getCurrentHierarchy()
    PixelClassifierTools.createAnnotationsFromPixelClassifier(hierarchy, thresholdServer, -1, -1)
    
    // Select current annotations
    selectObjectsByClassification(currClassName);
   
    // Get current annotations, rescale
    def oldObjects = getAnnotationObjects().findAll{it.getPathClass() == getPathClass(currClassName)}
    def transform = java.awt.geom.AffineTransform.getScaleInstance(downsample, downsample)
    def newObjects = oldObjects.collect {p -> PathObjectTools.transformObject(p, transform, false)}
    
    // Delete old annotations
    clearSelectedObjects(false);
    
    // add resulting annotation object (and update current threshold)
    addObjects(newObjects)
    current_thresh++;
}


// finally, correct all annotations, by iteratively subtracting adjacent annotations to assign one uint to one class
for(int i = 0; i < classNames.size() - 1; i++) {
    def className1 = classNames[i]
    def className2 = classNames[i + 1]
    
    def class1 = getAnnotationObjects().find {it.getPathClass() == getPathClass(className1)}
    def class2 = getAnnotationObjects().find {it.getPathClass() == getPathClass(className2)}
    def plane = class1.getROI().getImagePlane()
    if (plane != class2.getROI().getImagePlane()) {
        println 'Annotations are on different planes!'
        return    
    }
    // Convert to geometries & compute distance
    // Note: see https://locationtech.github.io/jts/javadoc/org/locationtech/jts/geom/Geometry.html#distance-org.locationtech.jts.geom.Geometry-
    def g1 = class1.getROI().getGeometry()
    def g2 = class2.getROI().getGeometry()
    
    def difference = g1.difference(g2)
    if (difference.isEmpty())
        println "No intersection between areas"
    else {
        def roi = GeometryTools.geometryToROI(difference, plane)
        def annotation = PathObjects.createAnnotationObject(roi, getPathClass('Difference'))
        addObject(annotation)
        selectObjects(annotation)
    }
    //remove original object
    removeObject(class1, true)
    
    // rename annotation
    getAnnotationObjects().each { annotation ->
    if (annotation.getPathClass().equals(getPathClass("Difference")))
        annotation.setPathClass(getPathClass(className1))
    }
}

print "Done!"

// reclaim memory - relevant for running this within a RunForProject
Thread.sleep(100);
javafx.application.Platform.runLater {
    getCurrentViewer().getImageRegionStore().cache.clear();
    System.gc();
}
Thread.sleep(100);