package gasgithub.minio.client.rest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirementsSet;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.Result;
import io.minio.errors.*;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;


@SecurityScheme(
    securitySchemeName = "accessKey",
    type = SecuritySchemeType.APIKEY,
    apiKeyName = "X-Access-Key",
    in = SecuritySchemeIn.HEADER
)
@SecurityScheme(
    securitySchemeName = "secretKey",
    type = SecuritySchemeType.APIKEY,
    apiKeyName = "X-Secret-Key",
    in = SecuritySchemeIn.HEADER
)

@Tag(name = "buckets", description = "Operations related to buckets browsing.")
@Tag(name = "download", description = "Operations related to downloading assets.")
@Tag(name = "upload", description = "Operations related to uploading assets.")


@SecurityRequirementsSet(
    value = {@SecurityRequirement(name="accessKey"), @SecurityRequirement(name="secretKey")}
 )
@Path("/") 
public class SimpleRestMinioClient {
    @Inject 
    @ConfigProperty(name="COS_ENDPOINT", defaultValue = "https://s3.us-east.cloud-object-storage.appdomain.cloud")
    private String COS_ENDPOINT;

    private MinioClient getMinioClient(String accessKey, String secretKey) {
        MinioClient minioClient = MinioClient.builder()
        .endpoint(COS_ENDPOINT)
        .credentials(accessKey, secretKey)
        .build();
        return minioClient;
    }

    @GET
    @Path("buckets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Lists buckets",
        description = "Lists all buckets accessible by the user"
        //tags = {"buckets"}
    )
    @Tag(ref = "buckets")
    @APIResponse(
        responseCode = "200",
        description = "Retruns all buckets accessible by the user as json",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = Map.class, example = " { \"buckets\" : [\"bucketA\", \"bucketB\"]}"))
    )
    public Map<String, List<String>> getBuckets(@Parameter(hidden = true) @HeaderParam("X-Access-Key") String accessKey,
                                 @Parameter(hidden = true) @HeaderParam("X-Secret-Key") String secretKey) {
        //Properties buckets = new Properties();
        Map<String, List<String>> buckets = new HashMap<>();

        System.out.println(accessKey + " : "+ secretKey);

        MinioClient minioClient = getMinioClient(accessKey, secretKey);

        List<Bucket> listBuckets = new ArrayList<>();
        try {
            listBuckets = minioClient.listBuckets();
            ArrayList<String> bucketNames = new ArrayList<>();
            for (Bucket bucket : listBuckets) {
                bucketNames.add(bucket.name());
            }
            buckets.put("buckets", bucketNames);
            System.out.println(buckets);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        
        return buckets;
    }



    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("buckets/{bucket}")
    @Tag(ref = "buckets")
    public JsonObject getItems(@Parameter(hidden = true) @HeaderParam("X-Access-Key") String accessKey,
            @Parameter(hidden = true) @HeaderParam("X-Secret-Key") String secretKey,
            @PathParam("bucket") String bucket) {
        System.out.println("Bucket:" + bucket);
        MinioClient minioClient = getMinioClient(accessKey, secretKey);

        // Lists objects information.
        Iterable<Result<Item>> results = minioClient.listObjects(
            ListObjectsArgs.builder().bucket(bucket).build());

        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        JsonObjectBuilder builder = factory.createObjectBuilder();
        JsonArrayBuilder itemArrayBuilder = factory.createArrayBuilder();
                
        for (Result<Item> result : results) {
            try {
                Item item = result.get();
                System.out.println("adding :" + item.objectName());
                itemArrayBuilder.add(
                    factory.createObjectBuilder()
                    .add("name", item.objectName())
                    .add("isdir", item.isDir())
                );
            } catch (InvalidKeyException | ErrorResponseException | IllegalArgumentException | InsufficientDataException
                    | InternalException | InvalidResponseException | NoSuchAlgorithmException | ServerException
                    | XmlParserException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        JsonArray jsonArray = itemArrayBuilder.build();
        System.out.println("jsonArray:" + jsonArray);
        builder.add("items", jsonArray);
        JsonObject jsonObject = builder.build();
        System.out.println("jsonObject:" + jsonObject);
        return jsonObject;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("buckets/{bucket}/{filepath : .+}")
    @Tag(ref = "buckets")
    public JsonObject getItem(@Parameter(hidden = true) @HeaderParam("X-Access-Key") String accessKey, 
                              @Parameter(hidden = true) @HeaderParam("X-Secret-Key") String secretKey, 
                              @PathParam("bucket") String bucket,
                              @PathParam("filepath") String filepath) {
        System.out.println("Bucket:" + bucket);
        System.out.println("filepath:" + filepath);

        MinioClient minioClient = getMinioClient(accessKey, secretKey);

        // Lists objects information.
        Iterable<Result<Item>> results = minioClient.listObjects(
            ListObjectsArgs.builder().bucket(bucket).prefix(filepath).build());

        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        JsonObjectBuilder builder = factory.createObjectBuilder();
        JsonArrayBuilder itemArrayBuilder = factory.createArrayBuilder();
        builder.add("items", itemArrayBuilder);

        System.out.println("results:" + results);
        for (Result<Item> result : results) {
            try {
                Item item = result.get();
                System.out.println("name:" + item.objectName());
                itemArrayBuilder.add(factory.createObjectBuilder()
                .add("name", item.objectName())
                .add("isdir", item.isDir())
                );
            } catch (InvalidKeyException | ErrorResponseException | IllegalArgumentException | InsufficientDataException
                    | InternalException | InvalidResponseException | NoSuchAlgorithmException | ServerException
                    | XmlParserException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // String fileList[] = { "file1", "file2"};
        // Properties files = new Properties();
        // files.put("files", fileList );
        JsonArray jsonArray = itemArrayBuilder.build();
        System.out.println("jsonArray:" + jsonArray);
        builder.add("items", jsonArray);
        JsonObject jsonObject = builder.build();
        System.out.println("jsonObject:" + jsonObject);
        return jsonObject;
    }



    @GET
    @Path("/download/{bucket}/{filename : .+}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Tag(ref = "download")
    public Response getFile(@Parameter(hidden = true) @HeaderParam("X-Access-Key") String accessKey, 
        @Parameter(hidden = true) @HeaderParam("X-Secret-Key") String secretKey, 
        @PathParam("bucket") String bucket, 
        @PathParam("filename") String filename) throws Exception {

        MinioClient minioClient = getMinioClient(accessKey, secretKey);
        System.out.println("filename: " + filename);

        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                Writer writer = new BufferedWriter(new OutputStreamWriter(os));

                // get object given the bucket and object name
                try (InputStream miniostream = minioClient.getObject(
                    GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(filename)
                    .build())) {
                    // Read data from stream
                    int len;
                    byte[] buffer = new byte[4096];
                    while ((len = miniostream.read(buffer, 0, buffer.length)) != -1) {
                        os.write(buffer, 0, len);
                    }
                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                writer.flush();
            }
        };

        return Response.ok(stream).build();
    }   

    @POST
    @Path("/upload/{bucket}/{filename : .+}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Tag(ref = "upload")
    public void uploadFile(@Parameter(hidden = true) @HeaderParam("X-Access-Key") String accessKey,
        @Parameter(hidden = true) @HeaderParam("X-Secret-Key") String secretKey, 
         @PathParam("bucket") String bucket, 
         @PathParam("filename") String filename,
         InputStream stream) throws Exception {
        
        
        MinioClient minioClient = getMinioClient(accessKey, secretKey);
        System.out.println("filename: " + filename);
        System.out.println("stream ava: " + stream.available());

        // Create object 'my-objectname' in 'my-bucketname' with content from the input stream.
        minioClient.putObject(
            PutObjectArgs.builder().bucket(bucket).object(filename).stream(
                stream,  -1, 10485760)
                .build());
            stream.close();
        System.out.println("my-objectname is uploaded successfully");
    }    
    
}
