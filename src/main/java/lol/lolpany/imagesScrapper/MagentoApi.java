package lol.lolpany.imagesScrapper;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface MagentoApi {
//    @GET("products/{user}/repos")
//    List<Call<String>> listProducts(@Path("user") String user);

    @POST("oauth/initiate")
    Call<RequestBody> initiate(@Header("Authorization") String authorizationHeader);

    @GET("products/{id}")
    Call<RequestBody> readProduct(@Path("id")long id);

    @PUT("products/{product}/images/{image}")
    void updateImage(@Path("product") long product, @Path("image") long image);
}
