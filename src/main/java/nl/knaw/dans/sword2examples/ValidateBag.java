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

public class ValidateBag {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.printf("Usage: java %s <bag file/dir>", ValidateBag.class.getName());
            System.exit(1);
        }
        var bag = new File(args[0]);
        var bagInTarget = Common.copyToBagDirectoryInTarget(bag);
        var zippedBagInTarget = new File(bagInTarget.toString() + ".zip");
        Common.zipDirectory(bagInTarget, zippedBagInTarget);
        Common.validateZip(zippedBagInTarget, new URI("https://dar.dans.knaw.nl/validate-dans-bag"));
    }
}
