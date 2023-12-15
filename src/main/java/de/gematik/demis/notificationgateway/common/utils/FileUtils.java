/*
 * Copyright [2023], gematik GmbH
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */

package de.gematik.demis.notificationgateway.common.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

/** Collection of methods to access files. */
@Slf4j
public final class FileUtils {
  private static final String FILE_SEPARATOR = "/";

  private FileUtils() {}

  /**
   * Returns a {@link InputStream} from a given path.
   *
   * @param path the File Path
   * @param withLegacyMode if true, opens the file as a Classpath resource. If false, loads it as
   *     FileInputStream.
   * @return a {@link InputStream} corresponding to the given path, null otherwise
   */
  @Nullable
  public static InputStream getFileInput(final String path, final boolean withLegacyMode) {
    try {
      if (withLegacyMode) {
        log.debug("legacy mode is activated");
        String currPath = path;
        if (!currPath.startsWith(FILE_SEPARATOR)) {
          // add a leading file separator of cause root for this class is the classes folder of the
          // generated files
          currPath = FILE_SEPARATOR + currPath;
        }
        return FileUtils.class.getResourceAsStream(currPath);
      }

      return new FileInputStream(path);

    } catch (final NullPointerException | FileNotFoundException e) {
      log.error("unable to load file from path {}", path);
      return null;
    }
  }
}
