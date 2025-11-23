import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.*;
import java.io.IOException;
import java.util.Iterator;

/**
 * GifSequenceWriter utility class to create animated GIF from frames.
 * Source: https://memorynotfound.com/generate-gif-image-java-delay-infinite-loop-example/
 */
public class GifSequenceWriter {
    protected ImageWriter gifWriter;
    protected ImageWriteParam imageWriteParam;
    protected IIOMetadata imageMetaData;

    /**
     * Creates a new GifSequenceWriter
     * 
     * @param outputStream the ImageOutputStream to be written to
     * @param imageType one of the imageTypes specified in BufferedImage
     * @param timeBetweenFramesMS the time between frames in milliseconds
     * @param loopContinuously whether the gif should loop repeatedly
     * @throws IOException if failure writing to output
     */
    public GifSequenceWriter(
            ImageOutputStream outputStream,
            int imageType,
            int timeBetweenFramesMS,
            boolean loopContinuously) throws IOException {
        gifWriter = getWriter();
        imageWriteParam = gifWriter.getDefaultWriteParam();
        ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(imageType);

        imageMetaData = gifWriter.getDefaultImageMetadata(imageTypeSpecifier, imageWriteParam);

        String metaFormatName = imageMetaData.getNativeMetadataFormatName();

        IIOMetadataNode root = (IIOMetadataNode) imageMetaData.getAsTree(metaFormatName);

        IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");

        graphicsControlExtensionNode.setAttribute("disposalMethod", "restoreToBackgroundColor");
        graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute("transparentColorFlag", "TRUE");
        graphicsControlExtensionNode.setAttribute("delayTime", Integer.toString(timeBetweenFramesMS / 10));
        graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");

        IIOMetadataNode appExtensionsNode = getNode(root, "ApplicationExtensions");

        IIOMetadataNode appExtensionNode = new IIOMetadataNode("ApplicationExtension");

        appExtensionNode.setAttribute("applicationID", "NETSCAPE");
        appExtensionNode.setAttribute("authenticationCode", "2.0");

        int loop = loopContinuously ? 0 : 1;

        appExtensionNode.setUserObject(new byte[] {
                0x1,
                (byte) (loop & 0xFF),
                (byte) ((loop >> 8) & 0xFF)
        });
        appExtensionsNode.appendChild(appExtensionNode);

        imageMetaData.setFromTree(metaFormatName, root);

        gifWriter.setOutput(outputStream);

        gifWriter.prepareWriteSequence(null);
    }

    public void writeToSequence(RenderedImage img) throws IOException {
        gifWriter.writeToSequence(new IIOImage(img, null, imageMetaData), imageWriteParam);
    }

    /**
     * Close this GifSequenceWriter object. This does not close the underlying stream, just finishes off the GIF.
     * @throws IOException if an I/O error occurs
     */
    public void close() throws IOException {
        gifWriter.endWriteSequence();
    }

    /**
     * Returns the first available GIF ImageWriter using ImageIO
     * @return a GIF ImageWriter object
     * @throws IOException if GIF writer not found
     */
    private static ImageWriter getWriter() throws IOException {
        Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("gif");
        if (!iter.hasNext()) {
            throw new IOException("No GIF Image Writers Exist");
        } else {
            return iter.next();
        }
    }

    /**
     * Returns an existing child node, or creates and returns a new child node (if the requested node does not exist).
     * @param rootNode the node to search for the child node.
     * @param nodeName the name of the child node.
     * @return the child node, if found or a new node created with the given name.
     */
    private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {
        int nNodes = rootNode.getLength();
        for (int i = 0; i < nNodes; i++) {
            if (rootNode.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
                return (IIOMetadataNode) rootNode.item(i);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        rootNode.appendChild(node);
        return node;
    }
}
