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

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.File;
import java.net.URI;

public class SequenceSimpleDeposit {

    public static void main(String[] args) throws Exception {
        ArgumentParser parser = ArgumentParsers.newFor("SequenceSimpleDeposit").build()
            .defaultHelp(true)
            .description("Send multiple bags to the SWORD2 service in sequence.");
        parser.addArgument("colIri")
            .help("Collection URL (Col-IRI)");
        parser.addArgument("user")
            .help("Username or the string 'API_KEY' if password is an API key");
        parser.addArgument("password")
            .help("Password or API key");
        parser.addArgument("bags")
            .nargs("+")
            .help("Bag directories");

        Namespace ns;
        try {
            ns = parser.parseArgs(args);
        }
        catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
            return;
        }

        final URI colIri = new URI(ns.getString("colIri"));
        final String uid = ns.getString("user");
        final String pw = ns.getString("password");
        final String[] bagNames = ns.getList("bags").toArray(new String[0]);

        System.out.println("Sending base revision of dataset ...");
        File baseBagDir = new File(bagNames[0]);
        File bagDirInTarget = Common.copyToBagDirectoryInTarget(baseBagDir);
        URI baseUri = SimpleDeposit.depositBagDir(bagDirInTarget, colIri, uid, pw);

        for (int i = 1; i < bagNames.length; ++i) {
            File bagDir = new File(bagNames[i]);
            bagDirInTarget = Common.copyToBagDirectoryInTarget(bagDir);
            Common.setBagIsVersionOf(bagDirInTarget, baseUri);
            SimpleDeposit.depositBagDir(bagDirInTarget, colIri, uid, pw);
        }
        System.exit(0);
    }
}
