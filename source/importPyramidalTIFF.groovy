/**
 * Script to import uint8 pyramidal TIFF images as annotations for QuPath >= v0.3*
 *
 * It is assumed that the TIFF lies in a user-defined directory with the same name but with the extension '.tiff'
 * 
 * Furthermore, we assume that the image is uint8, where each uint correspond to a different class (currently only supports one class, value != 0)
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
def masksPath = "C:/path-to-masks-dir"
def downsample = 1.0;
def extension = ".tiff"  // pyramidal TIFF
def className = "Epithelium"
// ----------------------------

// Get a list of image files, stopping early if none can be found
def dirOutput = new File(masksPath);
if (!dirOutput.isDirectory()) {
    print dirOutput + ' is not a valid directory!';
    return;
}

// get current WSI
def currWSIName = GeneralTools.getNameWithoutExtension(getProjectEntry().getImageName())
def currFullPath = masksPath + "/" + currWSIName + extension

// check if file exists, if no return
File file = new File(currFullPath)
if (!file.exists()) {
    print currFullPath + ' does not exist!';
    return;
}

// Ideally you'd use ImageIO.read(File)... but if it doesn't work we need this
def server = ImageServerProvider.buildServer(currFullPath, BufferedImage)
def region = RegionRequest.createInstance(server)
def img = server.readBufferedImage(region)
def band = ContourTracing.extractBand(img.getRaster(), 0)
def request = RegionRequest.createInstance(getCurrentServer(), downsample)
def annotations = ContourTracing.createAnnotations(band, request, 1, 1)
addObjects(annotations)

// finally, rename to class of interest
replaceClassification(null, className);

print "Done!"

// reclaim memory - relevant for running this within a RunForProject
Thread.sleep(100);
javafx.application.Platform.runLater {
    getCurrentViewer().getImageRegionStore().cache.clear();
    System.gc();
}
Thread.sleep(100);