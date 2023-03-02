/*
 * Copyright (C) 2022 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.sword2examples;

import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.reader.BagReader;
import gov.loc.repository.bagit.verify.BagVerifier;
import gov.loc.repository.bagit.writer.BagWriter;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import nl.knaw.dans.sword2examples.api.entry.Entry;
import nl.knaw.dans.sword2examples.api.entry.Link;
import nl.knaw.dans.sword2examples.api.statement.Feed;
import nl.knaw.dans.sword2examples.api.statement.FeedCategory;
import nl.knaw.dans.sword2examples.api.statement.FeedEntry;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.xml.sax.InputSource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Common {
    static final String BAGIT_URI = "http://purl.org/net/sword/package/BagIt";
    private static final BagVerifier bagVerifier = new BagVerifier();

    private static final int numberOfSecondBetweenStatusChecks = 3;

    private static final List<String> statuses = Arrays.asList("DRAFT", "UPLOADED", "FINALIZING", "SUBMITTED", "INVALID", "REJECTED", "FAILED", "PUBLISHED");

    /**
     * Assumes the entity is UTF-8 encoded text and reads it into a String.
     *
     * @param entity the http entity object
     * @return the entire http entity as a string
     * @throws IOException if an I/O error occurs
     */
    public static String readEntityAsString(HttpEntity entity) throws IOException {
        var bos = new ByteArrayOutputStream();
        IOUtils.copy(entity.getContent(), bos);
        return bos.toString(StandardCharsets.UTF_8);
    }

    public static Feed parseFeed(String text) {
        try {
            var context = JAXBContext.newInstance(Feed.class);
            return (Feed) context.createUnmarshaller().unmarshal(new StringReader(text));
        }
        catch (JAXBException e) {
            throw new RuntimeException("Unable to parse XML", e);
        }
    }

    public static Entry parseEntry(String text) {
        try {
            var context = JAXBContext.newInstance(Entry.class);
            return (Entry) context.createUnmarshaller().unmarshal(new StringReader(text));
        }
        catch (JAXBException e) {
            throw new RuntimeException("Unable to parse XML", e);
        }
    }

    static URI trackDeposit(CloseableHttpClient http, URI statUri) throws Exception {
        String bodyText;
        System.out.println(String.format("Start polling Stat-IRI for the current status of the deposit, waiting %d seconds before every request ...", numberOfSecondBetweenStatusChecks));
        while (true) {
            Thread.sleep(numberOfSecondBetweenStatusChecks * 1000);
            System.out.print("Checking deposit status ... ");
            try (CloseableHttpResponse response = http.execute(addXAuthorizationToRequest(new HttpGet(statUri)))) {
                System.out.print("(received response) ... ");
                if (response.getStatusLine().getStatusCode() != 200) {
                    System.out.println("Stat-IRI returned " + response.getStatusLine().getStatusCode());
                    System.exit(1);
                }
                bodyText = readEntityAsString(response.getEntity());
            }

            Feed statement = parseFeed(bodyText);
            FeedCategory category = statement.getCategory();

            if (category == null || category.getTerm() == null) {
                System.err.println("ERROR: NO STATE FOUND");
                System.exit(1);
            }
            String state = category.getTerm();
            if (state.equals("INVALID") || state.equals("REJECTED") || state.equals("FAILED")) {
                System.err.println("FAILURE. Complete statement follows:");
                System.err.println(bodyText);
                System.exit(3);
            }
            else if (state.equals("PUBLISHED")) {
                List<FeedEntry> entries = statement.getEntries();
                System.out.println("SUCCESS. ");
                if (entries.size() == 1) {
                    List<String> dois = getDois(entries.get(0));
                    int numDois = dois.size();
                    switch (numDois) {
                        case 1:
                            System.out.println("Dataset has been published as: <" + dois.get(0) + ">. ");
                            break;
                        case 0:
                            System.out.println("WARNING: No DOI found");
                            break;
                        default:
                            System.out.println("WARNING: More than one DOI found (" + numDois + "): ");
                            boolean first = true;
                            for (String doi : dois) {
                                if (first) {
                                    first = false;
                                }
                                else {
                                    System.out.print(", ");
                                }
                                System.out.print(doi + "");

                            }
                            System.out.println();
                            break;
                    }
                    List<String> nbns = getNbn(entries.get(0));
                    int numNbns = nbns.size();
                    switch (numNbns) {
                        case 1:
                            System.out.println("Dataset NBN: <" + nbns.get(0) + ">. ");
                            break;
                        case 0:
                            System.out.println("WARNING: No NBN found");
                            break;
                        default:
                            System.out.println("WARNING: More than one NBN found (" + nbns + "): ");
                            break;
                    }
                    System.out.println("Bag ID for this version of the dataset: " + entries.get(0).getId());
                }
                else {
                    System.out.println("WARNING: Found (" + entries.size() + ") entry's; should be ONE and only ONE");
                }
                String stateText = category.getValue();
                System.out.println("State description: " + stateText + "");
                System.out.println("Complete statement follows:");
                printXml(bodyText);
                return new URI(entries.get(0).getId());
            }
            else {
                System.out.println(state);
                if (!statuses.contains(state)) {
                    System.out.println("WARNING: unknown status!");
                }
            }
        }
    }

    public static List<String> getDois(FeedEntry entry) {
        return entry.getLinks().stream()
            .filter(l -> "self".equals(l.getRel()))
            .filter(l -> "doi.org".equals(l.getHref().getHost()))
            .map(l -> l.getHref().toASCIIString())
            .collect(Collectors.toList());
    }

    public static List<String> getNbn(FeedEntry entry) {
        return entry.getLinks().stream()
            .filter(l -> "self".equals(l.getRel()))
            .filter(l -> "www.persistent-identifier.nl".equals(l.getHref().getHost()))
            .map(l -> l.getHref().toASCIIString())
            .collect(Collectors.toList());
    }

    private static byte[] readChunk(InputStream is, int size) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] bytes = new byte[size];
        int c = is.read(bytes);
        bos.write(bytes, 0, c);
        return bos.toByteArray();
    }

    public static CloseableHttpClient createHttpClient(URI uri, String uid, String pw) {
        BasicCredentialsProvider credsProv = new BasicCredentialsProvider();
        credsProv.setCredentials(new AuthScope(uri.getHost(), uri.getPort()), new UsernamePasswordCredentials(uid, pw));
        return HttpClients.custom().setDefaultCredentialsProvider(credsProv).build();
    }

    public static CloseableHttpClient createHttpClient(URI uri) {
        return HttpClients.custom().build();
    }


    public static CloseableHttpResponse sendChunk(DigestInputStream dis, int size, String method, URI uri, String filename, String mimeType,
        CloseableHttpClient http, boolean inProgress) throws Exception {
        // System.out.println(String.format("Sending chunk to %s, filename = %s, chunk size = %d, MIME-Type = %s, In-Progress = %s ... ", uri.toString(),
        // filename, size, mimeType, Boolean.toString(inProgress)));
        byte[] chunk = readChunk(dis, size);
        String md5 = new String(Hex.encodeHex(dis.getMessageDigest().digest()));
        HttpUriRequest request = RequestBuilder.create(method).setUri(uri).setConfig(RequestConfig.custom()
                /*
                 * When using an HTTPS-connection EXPECT-CONTINUE must be enabled, otherwise buffer overflow may follow
                 */
                .setExpectContinueEnabled(true).build()) //
            .addHeader("Content-Disposition", String.format("attachment; filename=%s", filename)) //
            .addHeader("Content-MD5", md5) //
            .addHeader("Packaging", BAGIT_URI) //
            .addHeader("In-Progress", Boolean.toString(inProgress)) //
            .setEntity(new ByteArrayEntity(chunk, ContentType.create(mimeType))) //
            .build();
        CloseableHttpResponse response = http.execute(addXAuthorizationToRequest(request));
        // System.out.println("Response received.");
        return response;
    }

    private static HttpUriRequest addXAuthorizationToRequest(HttpUriRequest request) throws Exception {
        File autValueFile = new File("x-auth-value.txt");
        if (autValueFile.exists()) {
            request.addHeader("X-Authorization", FileUtils.readFileToString(autValueFile).trim());
        }
        return request;
    }

    public static void setBagIsVersionOf(File bagDir, URI versionOfUri) throws Exception {
        Bag bag = new BagReader().read(bagDir.toPath());
        bagVerifier.isValid(bag, false);
        bag.getMetadata().add("Is-Version-Of", versionOfUri.toASCIIString());
        BagWriter.write(bag, bagDir.toPath());
    }

    public static void zipDirectory(File dir, File zipFile) throws Exception {
        if (zipFile.exists()) {
            if (!zipFile.delete()) {
                System.err.println("Warning: delete action on zip returned false. ZIP may not have been deleted.");
            }
            ;
        }
        try (var zf = new ZipFile(zipFile)) {
            ZipParameters parameters = new ZipParameters();
            zf.addFolder(dir, parameters);
        }
    }

    /**
     * Copies bag to the folder "target" and extracts it, if it is a zipfile. Existing sub-directory of the same name will be overwritten.
     *
     * @param bag the bag file or folder
     * @return a bag directory under the "target" folder
     */
    public static File copyToBagDirectoryInTarget(File bag) throws Exception {
        File dirInTarget = null;
        if (bag.isDirectory()) {
            dirInTarget = new File("target", bag.getName());
            FileUtils.deleteQuietly(dirInTarget);
            FileUtils.copyDirectory(bag, dirInTarget);
        }
        else {
            try (var zf = new ZipFile(bag)) {
                if (!zf.isValidZipFile()) {
                    System.err.println("ERROR: The submitted bag is not a valid directory or Zipfile");
                    System.exit(1);
                }
                else {
                    var zipInTarget = new File("target", bag.getName());
                    FileUtils.deleteQuietly(zipInTarget);
                    dirInTarget = new File("target", ZipUtil.getBaseDirName(bag.toString()));
                    FileUtils.deleteQuietly(dirInTarget);
                    zf.extractAll("target");
                }
            }
        }
        return dirInTarget;
    }

    public static void validateZip(File zippedBag, URI uri) throws Exception {
        try (CloseableHttpClient httpClient = createHttpClient(uri)) {
            var post = RequestBuilder
                .post(uri)
                .setHeader("Content-Type", "application/zip")
                .setEntity(new FileEntity(zippedBag)).build();
            var response = httpClient.execute(addXAuthorizationToRequest(post));
            var responseText = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            System.out.println(responseText);
        }
    }

    public static void printXml(String xml) {
        System.out.println("-- XML: START -------");
        System.out.println(prettyPrintByTransformer(xml, 2, false));
        System.out.println("-- XML: END ---------");
        System.out.println();
    }

    // From: https://www.baeldung.com/java-pretty-print-xml
    private static String prettyPrintByTransformer(String xmlString, int indent, boolean ignoreDeclaration) {

        try {
            InputSource src = new InputSource(new StringReader(xmlString));
            org.w3c.dom.Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, ignoreDeclaration ? "yes" : "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            Writer out = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(out));
            return out.toString();
        }
        catch (Exception e) {
            throw new RuntimeException("Error occurs when pretty-printing xml:\n" + xmlString, e);
        }
    }

    public static Optional<Link> getLinkByRel(List<Link> links, String rel) {
        return links.stream()
            .filter(f -> rel.equals(f.getRel()))
            .findFirst();
    }

}

