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
import gov.loc.repository.bagit.writer.BagWriter;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.abdera.parser.Parser;
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
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.util.ArrayList;
import java.util.List;

public class Common {
    static final String BAGIT_URI = "http://purl.org/net/sword/package/BagIt";

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

    public static <T extends Element> T parse(String text) {
        Abdera abdera = Abdera.getInstance();
        Parser parser = abdera.getParser();
        Document<T> receipt = parser.parse(new StringReader(text));
        return receipt.getRoot();
    }

    static URI trackDeposit(CloseableHttpClient http, URI statUri) throws Exception {
        CloseableHttpResponse response;
        String bodyText;
        System.out.println("Start polling Stat-IRI for the current status of the deposit, waiting 10 seconds before every request ...");
        while (true) {
            Thread.sleep(10000);
            System.out.print("Checking deposit status ... ");
            response = http.execute(addXAuthorizationToRequest(new HttpGet(statUri)));
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("Stat-IRI returned " + response.getStatusLine().getStatusCode());
                System.exit(1);
            }
            bodyText = readEntityAsString(response.getEntity());
            Feed statement = parse(bodyText);
            List<Category> states = statement.getCategories("http://purl.org/net/sword/terms/state");
            if (states.isEmpty()) {
                System.err.println("ERROR: NO STATE FOUND");
                System.exit(1);
            }
            else if (states.size() > 1) {
                System.err.println("ERROR: FOUND TOO MANY STATES (" + states.size() + "). CAN ONLY HANDLE ONE");
                System.exit(1);
            }
            else {
                String state = states.get(0).getTerm();
                System.out.println(state);
                if (state.equals("INVALID") || state.equals("REJECTED") || state.equals("FAILED")) {
                    System.err.println("FAILURE. Complete statement follows:");
                    System.err.println(bodyText);
                    System.exit(3);
                }
                else if (state.equals("PUBLISHED")) {
                    List<Entry> entries = statement.getEntries();
                    System.out.println("SUCCESS. ");
                    if (entries.size() == 1) {
                        System.out.print("Deposit has been archived at: <" + entries.get(0).getId() + ">. ");

                        List<String> dois = getDois(entries.get(0));
                        int numDois = dois.size();
                        switch (numDois) {
                            case 1:
                                System.out.print(" With DOI: [" + dois.get(0) + "]. ");
                                break;
                            case 0:
                                System.out.println("WARNING: No DOI found");
                                break;

                            default:
                                System.out.println("WARNING: More than one DOI found (" + numDois + "): ");
                                boolean first = true;
                                for (String doi : dois) {
                                    if (first)
                                        first = false;
                                    else
                                        System.out.print(", ");
                                    System.out.print(doi + "");

                                }
                                System.out.println();
                                break;
                        }
                    }
                    else {
                        System.out.println("WARNING: Found (" + entries.size() + ") entry's; should be ONE and only ONE");
                    }
                    String stateText = states.get(0).getText();
                    System.out.println("Dataset landing page will be located at: <" + stateText + ">.");
                    System.out.println("Complete statement follows:");
                    System.out.println(bodyText);
                    return entries.get(0).getId().toURI();
                }
                else if (!"SUBMITTED".equals(state)) {
                    System.out.println("Unknown status: " + state);
                }
            }
        }
    }

    public static List<String> getDois(Entry entry) {
        List<String> dois = new ArrayList<String>();

        List<Link> links = entry.getLinks("self");
        for (Link link : links) {
            IRI href = link.getHref();
            if (href.getHost().equals("doi.org")) {
                String path = href.getPath();
                String doi = path.substring(1); // skip leading '/'
                dois.add(doi);
            }
        }
        return dois;
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
        Bag bag = new Bag();
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
        try (CloseableHttpClient httpClient = createHttpClient(uri, "", "")) {
            var post = RequestBuilder
                .post(uri)
                .setHeader("Content-Type", "application/zip")
                .setEntity(new InputStreamEntity(new FileInputStream(zippedBag))).build();
            var response = httpClient.execute(post);
            var responseText = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            System.out.println(responseText);
        }
    }
}

