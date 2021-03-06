/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.uiserver.internal.io.reference;

import org.wso2.carbon.uiserver.internal.exception.FileOperationException;
import org.wso2.carbon.uiserver.internal.io.util.PathUtils;
import org.wso2.carbon.uiserver.internal.reference.FileReference;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A reference to a file inside a web app artifact..
 *
 * @since 0.8.0
 */
public class ArtifactFileReference implements FileReference {

    private final Path filePath;

    /**
     * Creates a reference to the file specified by the path.
     *
     * @param filePath path to the file
     */
    public ArtifactFileReference(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public String getName() throws FileOperationException {
        return PathUtils.getName(filePath);
    }

    @Override
    public String getExtension() throws FileOperationException {
        return PathUtils.getExtension(filePath);
    }

    @Override
    public String getContent() throws FileOperationException {
        try {
            return new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FileOperationException("Cannot read content of file '" + filePath + "'.", e);
        }
    }

    @Override
    public String getFilePath() throws FileOperationException {
        return filePath.toString();
    }
}
