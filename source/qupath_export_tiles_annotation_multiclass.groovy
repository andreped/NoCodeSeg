import qupath.lib.images.servers.LabeledImageServer

def imageData = getCurrentImageData()

// Define output path (relative to project)
def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())

//create directory called tiles in qupath project directory
//adding more strings will create more subfolders
def pathOutput = buildFilePath(PROJECT_BASE_DIR, 'tiles')
mkdirs(pathOutput)


// Define output resolution
double requestedPixelSize = 1 // default 10.0, change based on image size

// Convert to downsample
double downsample = requestedPixelSize / imageData.getServer().getPixelCalibration().getAveragedPixelSize()

// Create an ImageServer where the pixels are derived from annotations
def labelServer = new LabeledImageServer.Builder(imageData)
    .backgroundLabel(0, ColorTools.WHITE) // Specify background label (usually 0 or 255)
    .downsample(downsample)    // Choose server resolution; this should match the resolution at which tiles are exported
    .addLabel('Epithelia', 1)      // Choose output labels (the order matters! and so does the threshold value)
    .addLabel('Crypt', 2)
    .multichannelOutput(true)  // If true, each label is a different channel (required for multiclass probability)
    .build()

// Create an exporter that requests corresponding tiles from the original & labeled image servers
new TileExporter(imageData)
    .downsample(downsample)     // Define export resolution
    .imageExtension('.tif')     // Define file extension for original pixels (often .tif, .jpg, '.png' or '.ome.tif')
    .tileSize(512)              // Define size of each tile, in pixels
    .labeledServer(labelServer) // Define the labeled image server to use (i.e. the one we just built)
    .annotatedTilesOnly(false)  // If true, only export tiles if there is a (labeled) annotation present
    .overlap(64) // Define overlap, in pixel units at the export resolution
    .imageSubDir("images")//save images in a subfolder called images
    .labeledImageSubDir("masks")//save masks in a subfolder called masks             
    .writeTiles(pathOutput) // Write tiles to the specified directory: 
//by default you can omit imageSubDir and labeledImageSubDir and use just writeTiles.
//This will save all images together 
print 'Done!'