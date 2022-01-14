/**
 * Script to export annotations as labeled tiles for QuPath > 0.2*.
 *
 * All patches will be exported to the same directory called 'tiles' inside the Project directory
 * The patches will be filtered based on tissue content, and finally moved to respective the
 * subdirectories: Images and Labels within the 'tiles' folder
 *
 * Each patch's filename contains the original WSI ID, and images are saved as PNG (by default)
 * and ground truth as TIF
 *
 * The downsampling level can be set by the user, default value is 4.
 *
 * Code is inspired by the script from the QuPath documentations, written by Pete Bankhead:
 * https://qupath.readthedocs.io/en/stable/docs/advanced/exporting_images.html#tile-exporter
 *
 * @author André Pedersen
 */


import qupath.lib.images.servers.LabeledImageServer
import java.awt.image.Raster
import javax.imageio.ImageIO;


// ----- SET THESE PARAMETERS -----
def classNames = ["Benign", "Malign"]   // names of classes of interest (simply add more values to list to add more classes)
double downsample = 4  // which downsampling level to use -> how much to downsample the patches
double glassThreshold = 50  // which threshold to use for tissue detection (lower value => more tissue included in mask)
double percentageThreshold = 0.25  // if a patch contains less than X amount of tissue, it should be neglected (range [0.0, 1.0])
int patchSize = 512  // generated patch size
int pixelOverlap = 128  // stride for which patches are generated
def imageExtension = ".tif"
int nb_channels = 3;
// --------------------------------


def imageData = getCurrentImageData()

// Define output path (relative to project)
def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
def pathOutput = buildFilePath(PROJECT_BASE_DIR, 'tiles')
mkdirs(pathOutput)

// Create an ImageServer where the pixels are derived from annotations
def tempServer = new LabeledImageServer.Builder(imageData)
    .backgroundLabel(0, ColorTools.WHITE) // Specify background label (usually 0 or 255)
    .downsample(downsample)    // Choose server resolution; this should match the resolution at which tiles are exported
    .multichannelOutput(false)  // If true, each label is a different channel (required for multiclass probability)

// assign each class to the server (need to iterate across list array due to multi-class)
def counter = 1
classNames.each { currClassName ->
    tempServer.addLabel(currClassName, 1)  // Choose output labels (the order matters!)
    counter++;
}

// finally, build server
def labelServer = tempServer.build()

// Create an exporter that requests corresponding tiles from the original & labeled image servers
new TileExporter(imageData)
    .downsample(downsample)          // Define export resolution
    .imageExtension(imageExtension)  // Define file extension for original pixels (often .tif, .jpg, '.png' or '.ome.tif')
    .tileSize(patchSize)             // Define size of each tile, in pixels
    .labeledServer(labelServer)      // Define the labeled image server to use (i.e. the one we just built)
    .annotatedTilesOnly(true)        // If true, only export tiles if there is a (labeled) annotation present
    .overlap(pixelOverlap)           // Define overlap, in pixel units at the export resolution
    .writeTiles(pathOutput)          // Write tiles to the specified directory

// create new folder (IMAGES AND LABELS), but only if they do not exist!
def dir1 = new File(pathOutput + "/Images");
if (!dir1.isDirectory())
    dir1.mkdir()
    
def dir2 = new File(pathOutput + "/Labels");
if (!dir2.isDirectory())
    dir2.mkdir()

// attempt to delete unwanted patches, both some formats as well as patches containing mostly glass
// Iterate through all your tiles
File folder = new File(pathOutput)
File[] listOfFiles = folder.listFiles()

// for each patch
listOfFiles.each { tile ->
    // skip directories within masks folder, and skip all ground truth patches
    if (tile.isDirectory())
        return;
    def currPath = tile.getPath()
    if (!currPath.endsWith(imageExtension))
        return;
    
    // load TIFF images back again, estimate patch glass density, and remove patches with lots
    // of glass based on user-defined threshold
    def image = ImageIO.read(new File(currPath))
    Raster raster = image.getRaster();
    
    // estimate amount of tissue in patch
    def tissue = 0;
    for (int y = 0; y < image.getHeight(); ++y) {
        for (int x = 0; x < image.getWidth(); ++x) {
            double currDist = 0
            for (int z = 0; z < nb_channels; ++z) {
                currDist += raster.getSample(x, y, z)
            }
            currDist = ((currDist / 3) > (255 - glassThreshold)) ? 0 : 1;
            if (currDist > 0) {
                ++tissue
            }
        }
    }
    
    // remove patches containing less tissue, dependent on user-defined threshold, and move accepted patches to respective folders
    def amountTissue = tissue / (image.getWidth() * image.getHeight());
    def currLabelPatch = new File(pathOutput + "/" + tile.getName().split(imageExtension)[0] + ".png")
    if (amountTissue < percentageThreshold) {
        tile.delete()
        currLabelPatch.delete()
    } else {
        tile.renameTo(pathOutput + "/Images/" + tile.getName())
        currLabelPatch.renameTo(new File(pathOutput + "/Labels/" + tile.getName().split(imageExtension)[0] + ".png"))
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