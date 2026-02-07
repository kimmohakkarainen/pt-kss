package fi.publishertools.kss.model.serialization;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.SAXException;
import org.w3c.dom.Document;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.StoredFile;
import fi.publishertools.kss.util.XmlUtils;

/**
 * Serializes and deserializes ProcessingContext to/from binary format for development and debugging.
 * Uses Java object serialization (ObjectOutputStream/ObjectInputStream).
 */
public final class ProcessingContextSerializer {

    private ProcessingContextSerializer() {
    }

    /**
     * Serializes the given context to a binary file at the specified path.
     */
    public static void serialize(ProcessingContext context, Path path) throws Exception {
        ProcessingContextSnapshot snapshot = toSnapshot(context);
        Files.createDirectories(path.getParent());
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(path))) {
            oos.writeObject(snapshot);
        }
    }

    /**
     * Deserializes a ProcessingContext from a binary file at the specified path.
     */
    public static ProcessingContext deserialize(Path path) throws IOException, ParserConfigurationException, SAXException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(path))) {
            ProcessingContextSnapshot snapshot = (ProcessingContextSnapshot) ois.readObject();
            return fromSnapshot(snapshot);
        }
    }

    private static ProcessingContextSnapshot toSnapshot(ProcessingContext context) throws TransformerException {
        List<byte[]> storiesListBytes = null;
        List<Document> storiesList = context.getStoriesList();
        if (storiesList != null && !storiesList.isEmpty()) {
            storiesListBytes = new ArrayList<>(storiesList.size());
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            for (Document doc : storiesList) {
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                transformer.transform(new DOMSource(doc), new StreamResult(baos));
                storiesListBytes.add(baos.toByteArray());
            }
        }

        Map<String, String> serializableMetadata = filterMetadataToStringMap(context.getMetadata());

        return new ProcessingContextSnapshot(
                context.getFileId(),
                context.getOriginalFilename(),
                context.getContentType(),
                context.getFileSize(),
                context.getUploadTime(),
                context.getOriginalFileContents(),
                context.getPackageOpf(),
                storiesListBytes,
                context.getChapters(),
                context.getImageList(),
                context.getImageContent() != null ? new HashMap<>(context.getImageContent()) : null,
                context.getXhtmlContent(),
                context.getTocContent(),
                serializableMetadata);
    }

    private static Map<String, String> filterMetadataToStringMap(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Object> e : metadata.entrySet()) {
            Object v = e.getValue();
            if (v != null && v instanceof String s) {
                result.put(e.getKey(), s);
            }
        }
        return result.isEmpty() ? null : result;
    }

    private static ProcessingContext fromSnapshot(ProcessingContextSnapshot snapshot) throws ParserConfigurationException, SAXException, IOException {
        StoredFile storedFile = new StoredFile(
                snapshot.fileId(),
                snapshot.originalFilename(),
                snapshot.contentType(),
                snapshot.fileSize(),
                snapshot.uploadTime(),
                snapshot.originalFileContents());

        ProcessingContext context = new ProcessingContext(storedFile);

        context.setPackageOpf(snapshot.packageOpf());
        context.setStoriesList(parseStoriesList(snapshot.storiesList()));
        context.setChapters(snapshot.chapters());
        context.setImageList(snapshot.imageList());
        if (snapshot.imageContent() != null) {
            for (Map.Entry<String, byte[]> e : snapshot.imageContent().entrySet()) {
                context.addImageContent(e.getKey(), e.getValue());
            }
        }
        context.setXhtmlContent(snapshot.xhtmlContent());
        context.setTocContent(snapshot.tocContent());
        if (snapshot.metadata() != null) {
            for (Map.Entry<String, String> e : snapshot.metadata().entrySet()) {
                context.addMetadata(e.getKey(), e.getValue());
            }
        }
        return context;
    }

    private static List<Document> parseStoriesList(List<byte[]> storiesListBytes) throws ParserConfigurationException, SAXException, IOException {
        if (storiesListBytes == null || storiesListBytes.isEmpty()) {
            return null;
        }
        List<Document> result = new ArrayList<>(storiesListBytes.size());
        for (byte[] xmlBytes : storiesListBytes) {
            result.add(XmlUtils.parseXml(xmlBytes));
        }
        return result;
    }
}
