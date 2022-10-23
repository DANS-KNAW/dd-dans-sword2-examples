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

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Link;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public class SimpleDeposit {

    /**
     * Sends a bag to the easy-sword2 service and tracks its status until it is archived or failure is reported.
     * 
     * @param args
     *        0. collection URL (Col-IRI), 1. EASY user name, 2. EASY password, 3. bag to send (a directory or a zip file)
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("Usage: java nl.knaw.dans.easy.sword2examples.SimpleDeposit <Col-IRI> <EASY uid> <EASY passwd> <bag file/dir>");
            System.exit(1);
        }

        // Read command line arguments
        final IRI colIri = new IRI(args[0]);
        final String uid = args[1];
        final String pw = args[2];
        final String bagFile = args[3];

        File bagDirInTarget = Common.copyToBagDirectoryInTarget(new File(bagFile));
        depositPackage(bagDirInTarget, colIri, uid, pw);
    }

    public static URI depositPackage(File bagDir, IRI colIri, String uid, String pw) throws Exception {
        // 0. Zip the bag if it isn't yet.
        File zipFile = null;
        zipFile = new File(bagDir.getAbsolutePath() + ".zip");
        zipFile.delete();
        Common.zipDirectory(bagDir, zipFile);

        // 1. Set up stream for calculating MD5
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (FileInputStream fis = new FileInputStream(zipFile);
             DigestInputStream dis = new DigestInputStream(fis, md)) {

            // 2. Post entire bag to Col-IRI
            CloseableHttpClient http = Common.createHttpClient(colIri.toURI(), uid, pw);
            CloseableHttpResponse response = Common.sendChunk(dis, (int) zipFile.length(), "POST", colIri.toURI(), "bag.zip", "application/zip", http, false);

            // 3. Check the response. If transfer corrupt (MD5 doesn't check out), report and exit.
            String bodyText = Common.readEntityAsString(response.getEntity());
            if (response.getStatusLine().getStatusCode() != 201) {
                System.err.println("FAILED. Status = " + response.getStatusLine());
                System.err.println("Response body follows:");
                System.err.println(bodyText);
                System.exit(2);
            }
            System.out.println("SUCCESS. Deposit receipt follows:");
            System.out.println(bodyText);

            // 4. Get the statement URL. This is the URL from which to retrieve the current status of the deposit.
            System.out.println("Retrieving Statement IRI (Stat-IRI) from deposit receipt ...");
            Entry receipt = Common.parse(bodyText);
            Link statLink = receipt.getLink("http://purl.org/net/sword/terms/statement");
            IRI statIri = statLink.getHref();
            System.out.println("Stat-IRI = " + statIri);

            // 5. Check statement every ten seconds (a bit too frantic, but okay for this test). If status changes:
            // report new status. If status is an error (INVALID, REJECTED, FAILED) or ARCHIVED: exit.
            return Common.trackDeposit(http, statIri.toURI());
        }
    }
}
