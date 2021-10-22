/**
 * Script to import Pyramidal TIFF segmentations as overlays for QuPath > 0.2*
 *
 * It is assumed that the user has defined the location of the segmentation TIFF
 *
 * The segmentation TIFF should be paired with the correct WSI, hence, assumed that the original WSI is rendered
 *
 * Code is based on the script provided by Pete Bankhead on the ImageJ forum:
 * https://forum.image.sc/t/rendering-wsi-as-overlay-on-top-of-another-wsi/52629/7?u=andreped
 *
 * @author André Pedersen
 */


import qupath.lib.color.ColorModelFactory
import qupath.lib.gui.viewer.overlays.BufferedImageOverlay
import qupath.lib.images.servers.ImageServerProvider
import qupath.lib.regions.RegionRequest
import java.awt.image.BufferedImage
import static qupath.lib.gui.scripting.QPEx.*

// Example of corresponding (segmentation) TIFF image to visualize as overlay on top of
// the original WSI. Assuming that the segmentation TIFF is stored as a pyramidal, single-channel uint8.
// Each uint should represent a different class, and 0 is expected to be the background class, to not
// be visualized
def path = "C:/Users/andrp/workspace/FAST-Pathology/code-free-study/build/pred_seg.tiff"

// Ideally you'd use ImageIO.read(File)... but if it doesn't work we need this
def server = ImageServerProvider.buildServer(path, BufferedImage)
def region = RegionRequest.createInstance(server)
def img = server.readBufferedImage(region)

// Ideally we'd use the image as it is... but here we create a new color model with RGBA values
def colorModel = ColorModelFactory.createIndexedColorModel(
        [0: getColorRGB(0, 0, 0, 0), 1: getColorRGB(255, 0, 0, 127)],
        true)
img = new BufferedImage(colorModel, img.getRaster(), false, null)

// Show as a simple overlay, it will automatically rescale to the full image size
def viewer = getCurrentViewer()
def overlay = new BufferedImageOverlay(viewer, img)
viewer.setCustomPixelLayerOverlay(overlay)
