package de.gematik.demis.notificationgateway.common.utils;

/*-
 * #%L
 * DEMIS Notification-Gateway
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
 * %%
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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
 * #L%
 */

import jakarta.annotation.Nullable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
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
   * @return a {@link InputStream} corresponding to the given path, null otherwise
   */
  @Nullable
  public static InputStream loadFileFromPath(final String path) {
    try {
      return new FileInputStream(path);

    } catch (final NullPointerException | FileNotFoundException e) {
      log.error("Unable to load file from path {}", path);
      return null;
    }
  }

  /**
   * Returns a {@link InputStream} from classpath..
   *
   * @param path the File Path
   * @return a {@link InputStream} corresponding to the given path, null otherwise
   */
  @Nullable
  public static InputStream loadFileFromClasspath(final String path) {
    try {
      String currPath = path;
      if (!currPath.startsWith(FILE_SEPARATOR)) {
        // add a leading file separator of cause root for this class is the classes folder of the
        // generated files
        currPath = FILE_SEPARATOR + currPath;
      }
      return FileUtils.class.getResourceAsStream(currPath);
    } catch (final NullPointerException e) {
      log.error("Unable to load resource {}", path);
      return null;
    }
  }
}
