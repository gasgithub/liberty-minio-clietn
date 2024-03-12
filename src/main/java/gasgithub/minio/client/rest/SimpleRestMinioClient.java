package gasgithub.minio.client.rest;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

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
    public List<Bucket> getBuckets(@HeaderParam("X-Access-Key") String accessKey, @HeaderParam("X-Secret-Key") String secretKey) {
        //Properties buckets = new Properties();

        System.out.println(accessKey + " : "+ secretKey);

        MinioClient minioClient = getMinioClient(accessKey, secretKey);

        List<Bucket> listBuckets = new ArrayList<>();
        try {
            listBuckets = minioClient.listBuckets();
            System.out.println(listBuckets);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //String bucktes[] = { "my1", "my2"};
        //buckets.put("buckets", bucktes);
        return listBuckets;
    }



    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{bucket}")
    public Iterable<Result<Item>> getFiles(@HeaderParam("X-Access-Key") String accessKey, @HeaderParam("X-Secret-Key") String secretKey, @PathParam("bucket") String bucket) {
        System.out.println("Bucket:" + bucket);
        MinioClient minioClient = getMinioClient(accessKey, secretKey);

        // Lists objects information.
        Iterable<Result<Item>> results = minioClient.listObjects(
            ListObjectsArgs.builder().bucket(bucket).build());

        System.out.println("results:" + results);

        // String fileList[] = { "file1", "file2"};
        // Properties files = new Properties();
        // files.put("files", fileList );
        return results;
    }

    @GET
    @Path("{bucket}/{filename}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getFile(@HeaderParam("X-Access-Key") String accessKey, @HeaderParam("X-Secret-Key") String secretKey, @PathParam("bucket") String bucket, @PathParam("filename") String filename) throws Exception {
        MinioClient minioClient = getMinioClient(accessKey, secretKey);

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


                // try (InputStream is = new FileInputStream("E:\\tmp\\hcsd\\model-data\\mnist_test.csv")) {
                //     int len;
                //     byte[] buffer = new byte[4096];
                //     while ((len = is.read(buffer, 0, buffer.length)) != -1) {
                //         os.write(buffer, 0, len);
                //     }
                // }
                writer.flush();
            }
        };

        return Response.ok(stream).build();
    }   

    
}
