/**
 * Script to import binary masks & create annotations, adding them to the current object hierarchy for QuPath >= v0.3.*
 *
 * It is assumed that each mask is stored in a TIFF file in a project subdirectory called 'masks'.
 * Each file name should be of the form:
 *   "Labels_{Short original image name} [{x},{y}, {width},{height}].tif"
 *
 * Note: It's assumed that the classification is a simple name without underscores, i.e. not a 'derived' classification
 * (so 'Tumor' is ok, but 'Tumor: Positive' is not)
 *
 * It is also assumed that the background is assigned value 0 and class of interest is assigned value 1.
 *
 * The x, y, width & height values should be in terms of coordinates for the full-resolution image.
 *
 * By default, the image name stored in the mask filename has to match that of the current image - but this check can be turned off.
 *
 * Code is inspired by scripts and ideas from these threads of the ImageJ forum:
 * https://forum.image.sc/t/importing-binary-masks-in-qupath/25713/24
 * https://forum.image.sc/t/importing-binary-masks-in-qupath/25713/2
 * https://forum.image.sc/t/transferring-segmentation-predictions-from-custom-masks-to-qupath/43408/24
 *
 * The code of these threads were mainly contributed by Pete Bankhead, Benjamin Pavie, and Raymond301.
 *
 * @author André Pedersen
 */


import ij.plugin.filter.ThresholdToSelection
import ij.process.ImageProcessor
import qupath.lib.objects.PathObjects
import qupath.lib.regions.ImagePlane
import qupath.imagej.processing.RoiLabeling
import ij.IJ
import static qupath.lib.gui.scripting.QPEx.*


// --- SET THESE PARAMETERS ---
def className = "Epithelium";
def pathOutput = "C:/path-to-exported-labels-dir"
def patchFormat = "tif"  // set to "png" or "tif"
// ----------------------------


// Get the main QuPath data structures
def imageData = getCurrentImageData();
def hierarchy = imageData.getHierarchy();
def server = imageData.getServer();
def plane = getCurrentViewer().getImagePlane();

def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
print pathOutput
print name

// Get a list of image files, stopping early if none can be found
def dirOutput = new File(pathOutput);
print dirOutput
if (!dirOutput.isDirectory()) {
    print pathOutput + ' is not a valid directory!';
    return;
}

def files = dirOutput.listFiles({f -> f.isFile() } as FileFilter) as List;
if (files.isEmpty()) {
    print 'No mask files found in ' + dirOutput;
    return;
}

// loading bar
int spaces = 40;
float progress = 100.0;
int counter = 0;
int nbPatches = files.size;

// Create annotations for all the files
def annotations = [];
files.each {
    String hash = "#" * Math.ceil((counter * spaces) / nbPatches);
    println String.format("[%-" + spaces + "s] %d%s%d\r", hash, counter, '/', nbPatches);
    counter ++;
    def currentName = GeneralTools.getNameWithoutExtension(getProjectEntry().getImageName())
    def filename = it.getName();
    if (!filename.contains(currentName) || !filename.endsWith("]." + patchFormat))
        return;
    try {
        annotations << parseAnnotation(it, plane);
    } catch (Exception e) {
        print 'Unable to parse annotation from ' + it.getName() + ': ' + e.getLocalizedMessage();
    }
}

/**
 * Create a new annotation from a binary image, parsing the classification & region from the file name.
 *
 * @param file File containing the TIFF image mask.  The image name must be formatted as above.
 * @return The PathAnnotationObject created based on the mask & file name contents.
 */
def parseAnnotation(File file, ImagePlane plane) {
    
    def filename  = file.getName();
    def imp = IJ.openImage(file.getPath());
    
    def parts = filename.split(' ');
    def regionParts = parts[-1].split(",") as List;
    
    // Handle scenario where there was not done any downsampling (d=1 skipped!)
    if (regionParts.size() == 4) {
        regionParts[0] = regionParts[0][1..-1]
        regionParts.add(0, "[d=1")
    }
    
    def downsample = regionParts[0].replace("[d=", "") as float;

    // Parse the x, y coordinates of the region
    int x = regionParts[1].replace("x=", "") as int;
    int y = regionParts[2].replace("y=", "") as int;
    
    // To create the ROI, travel into ImageJ
    def bp = imp.getProcessor();
    int n = bp.getStatistics().max as int;
    def rois = RoiLabeling.labelsToConnectedROIs(bp, n);
    
    def pathObjects = rois.collect {
        if (it == null)
           return;
       def roiQ = IJTools.convertToROI(it, -x/downsample, -y/downsample, downsample, plane);
       return PathObjects.createAnnotationObject(roiQ);
    }
    // this is slow, but it works... Better to add objects AFTER, as it seems like it draws every single time, maybe?
    addObjects(pathObjects);
}

resolveHierarchy();

// finally, rename to class of interest
replaceClassification(null, className);

// merge all annotations
print "Merging (might take some time...)"
selectObjects {
   return it.isAnnotation() && it.getPathClass() == getPathClass(className)
}
mergeSelectedAnnotations();

print "Done!"

// reclaim memory - relevant for running this within a RunForProject
Thread.sleep(100);
javafx.application.Platform.runLater {
    getCurrentViewer().getImageRegionStore().cache.clear();
    System.gc();
}
Thread.sleep(100);