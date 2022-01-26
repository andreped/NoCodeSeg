/**
 * Script to import uint8 stitched TIF images as annotations for QuPath >= v0.3*
 *
 * It is assumed that the TIF lies in a user-defined directory with the same name but with the extension '.tif'
 *
 * However, the name of the stitched images (assumed .tif) can be set in the "extensions" variable
 * 
 * Furthermore, we assume that the image is uint8, where each uint correspond to a different class
 * 
 * Script supports multi-class stitched images, but it is assumed that each class is set in a strided manner
 * where uint 0 is assumed to be background
 * 
 * Code is inspired by Pete Bankhead's script from the ImageJ forum:
 * https://forum.image.sc/t/rendering-wsi-as-overlay-on-top-of-another-wsi/52629/9?u=andreped
 *
 * @author André Pedersen
 */


import qupath.lib.images.servers.ImageServerProvider
import qupath.lib.regions.RegionRequest
import java.awt.image.BufferedImage
import qupath.lib.analysis.images.ContourTracing;
import static qupath.lib.gui.scripting.QPEx.*


// --- SET THESE PARAMETERS ---
def masksPath = "E:/path/to/some/ResultsModels_WSI/"
def downsample = 2.0;
def extension = ".tif"
def classNames = ["Benign", "Malign"]  // example class names, for single class use ["Benign"]
boolean fromFP = false  // if predictions are stored in a FP-like manner (two-level folder structure = true)
boolean fromMIB = true  // if predictions are from MIB, they will need to be renamed adding "labels_"
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

def path = ""
if (fromMIB) {
    path = masksPath + "/labels_" + currWSIName + extension
} else {
    path = masksPath + "/" + currWSIName + extension
}

// check if file exists, if no return
File file = new File(path)
if (!file.exists()) {
    print path + ' does not exist!';
    return;
}

// Ideally you'd use ImageIO.read(File)... but if it doesn't work we need this
def server = ImageServerProvider.buildServer(path, BufferedImage)
def region = RegionRequest.createInstance(server)
def img = server.readBufferedImage(region)
def band = ContourTracing.extractBand(img.getRaster(), 0)
def request = RegionRequest.createInstance(getCurrentServer(), downsample)

// for each class, iterate and create annotations for each
int counter = 1
classNames.each { currClassName ->
    def annotations = ContourTracing.createAnnotations(band, request, counter, counter)
    
    addObjects(annotations)
    replaceClassification(null, currClassName);
    
    counter++
}

print "Done!"

// reclaim memory - relevant for running this within a RunForProject
Thread.sleep(100);
javafx.application.Platform.runLater {
    getCurrentViewer().getImageRegionStore().cache.clear();
    System.gc();
}
Thread.sleep(100);