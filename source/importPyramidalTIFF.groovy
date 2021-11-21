 /**
 * Script to import uint8 pyramidal TIFF images as annotations for QuPath >= v0.3*
 *
 * It is assumed that the TIFF lies in a user-defined directory with the same name but with the extension '.tiff'
 * 
 * Furthermore, we assume that the image is uint8, where each uint correspond to a different class (currently only supports one class, value != 0)
 *
 * Code is slightly modified from Pete Bankhead's script to support RunForProject:
 * https://gist.github.com/petebankhead/27c1f8cd950583452c756f3a2ea41fc0
 *
 * which was written for https://forum.image.sc/t/rendering-wsi-as-overlay-on-top-of-another-wsi/52629/25?u=andreped
 *
 * @author André Pedersen
 */


 // --- SET THESE PARAMETERS ---
def masksPath = "C:/path-to-masks-dir"  // path to where TIFFs are stored
def level = 0                           // which level to extract segmentation from (choosing 0 may be slow)
def extension = ".tiff"                 // pyramidal TIFF
def className = "Epithelium"            // name of class of interest
double threshold = 0.5                  // Threshold value
int channel = 0                         // 0-based index for the channel to threshold
boolean fromFP = true                   // if TIFF is created in FastPathology, should set this to 'true'
// ----------------------------


def belowClass = getPathClass('Ignore*')     // Class for pixels below the threshold
def aboveClass = getPathClass(className)     // Class for pixels above the threshold

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

// Create a thresholded image
def thresholdServer = PixelClassifierTools.createThresholdServer(server, channel, threshold, belowClass, aboveClass)

// Create annotations and add to the current object hierarchy
def hierarchy = getCurrentHierarchy()
PixelClassifierTools.createAnnotationsFromPixelClassifier(hierarchy, thresholdServer, -1, -1)

print "Done!"

// reclaim memory - relevant for running this within a RunForProject
Thread.sleep(100);
javafx.application.Platform.runLater {
    getCurrentViewer().getImageRegionStore().cache.clear();
    System.gc();
}
Thread.sleep(100);