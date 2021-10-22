/**
 * Script to export annotations as labeled tiles for QuPath > 0.2*
 *
 * All patches will be exported to the same directory called 'tiles' inside the Project directory
 *
 * Each patch's filename contains the original WSI ID, and are saved as JPEG images
 *
 * The downsampling level can be set by the user, default value is 4.
 *
 * By default, the image name stored in the mask filename has to match that of the current image - but this check can be turned off.
 *
 * Code is inspired by the script from the QuPath documentations, written by Pete Bankhead:
 * https://qupath.readthedocs.io/en/stable/docs/advanced/exporting_images.html#tile-exporter
 *
 * @author André Pedersen
 */
 
 //Requirement:
//QuPath version > 0.2*
//See https://qupath.readthedocs.io/en/latest/docs/scripting/overview.html


import qupath.lib.images.servers.LabeledImageServer

def imageData = getCurrentImageData()

// Define output path (relative to project)
def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
def pathOutput = buildFilePath(PROJECT_BASE_DIR, 'tiles', name)
mkdirs(pathOutput)

// Convert to downsample
double downsample = 4

// Create an ImageServer where the pixels are derived from annotations
def labelServer = new LabeledImageServer.Builder(imageData)
    .backgroundLabel(0, ColorTools.WHITE) // Specify background label (usually 0 or 255)
    .downsample(downsample)    // Choose server resolution; this should match the resolution at which tiles are exported
    .addLabel('Epithelium', 1)      // Choose output labels (the order matters!)
    .multichannelOutput(true)  // If true, each label is a different channel (required for multiclass probability)
    .build()

// Create an exporter that requests corresponding tiles from the original & labeled image servers
new TileExporter(imageData)
    .downsample(downsample)     // Define export resolution
    .imageExtension('.jpg')     // Define file extension for original pixels (often .tif, .jpg, '.png' or '.ome.tif')
    .tileSize(512)              // Define size of each tile, in pixels
    .labeledServer(labelServer) // Define the labeled image server to use (i.e. the one we just built)
    .annotatedTilesOnly(false)  // If true, only export tiles if there is a (labeled) annotation present
    .overlap(128)                // Define overlap, in pixel units at the export resolution
    .writeTiles(pathOutput)     // Write tiles to the specified directory

// delete exported patches (only keep annotations), and remove ground truth tiles of small size (mostly glass)
File folder = new File(pathOutput)
File[] listOfFiles = folder.listFiles()
listOfFiles.each { tile ->
    def fullPath = pathOutput + "/" + tile.getName();
    print fullPath.length()
    if (fullPath.endsWith(".jpg") || (fullPath.length() / 1024 < 6))
        boolean fileSuccessfullyDeleted =  new File(fullPath).delete()
}

print "Done!"

// relevant for running this within a RunForProject
Thread.sleep(100);
javafx.application.Platform.runLater {
    getCurrentViewer().getImageRegionStore().cache.clear();
    System.gc();
}
Thread.sleep(100);
