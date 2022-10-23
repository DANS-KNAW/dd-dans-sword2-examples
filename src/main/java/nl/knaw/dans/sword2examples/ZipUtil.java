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

import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipUtil {

    public static String getBaseDirName(String zipFilePath) throws Exception {

        try (ZipFile zipFile = new ZipFile(zipFilePath)) {

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            ZipEntry entry = entries.nextElement();
            String[] entryDir = entry.getName().split("/", 0);

            while (entries.hasMoreElements()) {
                ZipEntry nextEntry = entries.nextElement();
                String[] nextEntryDir = nextEntry.getName().split("/", 0);
                if (!entryDir[0].equals(nextEntryDir[0])) {
                    System.err.println("ERROR: A bag may only contain a single root directory, please make sure " + zipFilePath + " adheres to the format. Aborting dataset submission." );
                    System.exit(1);
                }
            }
            return entryDir[0];
        }
    }
}
