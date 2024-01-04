/*
 * Copyright 2017-2024 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example.micronaut;

import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.oracle.bmc.objectstorage.responses.PutObjectResponse;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.http.server.types.files.StreamedFile;
import io.micronaut.http.server.util.HttpHostResolver;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.objectstorage.oraclecloud.OracleCloudStorageEntry;
import io.micronaut.objectstorage.oraclecloud.OracleCloudStorageOperations;
import io.micronaut.objectstorage.request.UploadRequest;
import io.micronaut.objectstorage.response.UploadResponse;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;

import java.net.URI;
import java.util.Optional;

//tag::begin-class[]
@Controller(ProfilePicturesController.PREFIX) // <1>
@ExecuteOn(TaskExecutors.BLOCKING) // <2>
public class ProfilePicturesController implements ProfilePicturesApi {

    static final String PREFIX = "/pictures";

    private final OracleCloudStorageOperations objectStorage; // <3>
    private final HttpHostResolver httpHostResolver; // <4>

    public ProfilePicturesController(OracleCloudStorageOperations objectStorage, HttpHostResolver httpHostResolver) {
        this.objectStorage = objectStorage;
        this.httpHostResolver = httpHostResolver;
    }
//end::begin-class[]

    //tag::upload[]
    @Override
    public HttpResponse<?> upload(CompletedFileUpload fileUpload, String userId, HttpRequest<?> request) {
        String key = buildKey(userId); // <1>
        UploadRequest objectStorageUpload = UploadRequest.fromCompletedFileUpload(fileUpload, key); // <2>
        UploadResponse<PutObjectResponse> response = objectStorage.upload(objectStorageUpload);  // <3>

        return HttpResponse
                .created(location(request, userId)) // <4>
                .header(HttpHeaders.ETAG, response.getETag()); // <5>
    }

    private static String buildKey(String userId) {
        return userId + ".jpg";
    }

    private URI location(HttpRequest<?> request, String userId) {
        return UriBuilder.of(httpHostResolver.resolve(request))
                .path(PREFIX)
                .path(userId)
                .build();
    }
    //end::upload[]

    //tag::download[]
    @Override
    public Optional<HttpResponse<StreamedFile>> download(String userId) {
        String key = buildKey(userId);
        return objectStorage.retrieve(key) // <1>
                .map(ProfilePicturesController::buildStreamedFile); // <2>
    }

    private static HttpResponse<StreamedFile> buildStreamedFile(OracleCloudStorageEntry entry) {
        GetObjectResponse nativeEntry = entry.getNativeEntry();
        MediaType mediaType = MediaType.of(nativeEntry.getContentType());
        StreamedFile file = new StreamedFile(entry.getInputStream(), mediaType).attach(entry.getKey());
        MutableHttpResponse<Object> httpResponse = HttpResponse.ok()
                .header(HttpHeaders.ETAG, nativeEntry.getETag()); // <3>
        file.process(httpResponse);
        return httpResponse.body(file);
    }
    //end::download[]

    //tag::delete[]
    @Override
    public void delete(String userId) {
        String key = buildKey(userId);
        objectStorage.delete(key);
    }
    //end::delete[]

//tag::end-class[]
}
//end::end-class[]
