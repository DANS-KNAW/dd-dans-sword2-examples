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

import java.io.File;
import java.net.URI;

public class SequenceContinuedDeposit {

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err
                .printf("Usage: %s <Col-IRI> <user> <passwd> <chunk size> <bag dirname>...%n", SequenceContinuedDeposit.class.getName());
            System.err
                .printf("Usage: %s <Col-IRI> API_KEY <apikey> <chunk size> <bag dirname>...%n", SequenceContinuedDeposit.class.getName());
            System.exit(1);
        }

        // 0. Read command line arguments
        final URI uri = new URI(args[0]);
        final String uid = args[1];
        final String pw = args[2];
        final int chunkSize = Integer.parseInt(args[3]);

        final String[] bagNames = new String[args.length - 4];
        System.arraycopy(args, 4, bagNames, 0, bagNames.length);

        System.out.println("Sending base revision of dataset ...");
        File baseBagDir = new File(bagNames[0]);
        File bagDirInTarget = Common.copyToBagDirectoryInTarget(baseBagDir);
        URI baseUri = ContinuedDeposit.depositPackage(bagDirInTarget, uri, uid, pw, chunkSize);

        for (int i = 1; i < bagNames.length; ++i) {
            File bagDir = new File(bagNames[i]);
            bagDirInTarget = Common.copyToBagDirectoryInTarget(bagDir);
            Common.setBagIsVersionOf(bagDirInTarget, baseUri);
            ContinuedDeposit.depositPackage(bagDirInTarget, uri, uid, pw, chunkSize);
        }
        System.exit(0);
    }
}
