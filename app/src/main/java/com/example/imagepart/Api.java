package com.example.imagepart;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface Api {

    @Multipart
    @POST("/Services/SignUpFile")
    Call<ResponseBody> registrationPart(@Header("Authorization") String Authorization, @Header("x-api-key") String x_api_key, @Part("name") String name,
                                        @Part("email") String email, @Part("mobile") String mobile, @Part("emergencyNumber") String emergencyNumber,
                                        @Part("password") String password, @Part("deviceid") String deviceid, @Part("role") String role,
                                        @Part("registerFrom") String registerFrom, @Part("AppId") String AppId,
                                        @Part MultipartBody.Part filename);

    @Multipart
    @POST("/Services/SignUpFile")
    Call<ResponseBody> registrationImagePart(@Header("Authorization") String Authorization, @Header("x-api-key") String x_api_key, @Part("name") RequestBody name,
                                             @Part("email") RequestBody email, @Part("mobile") RequestBody mobile, @Part("emergencyNumber") RequestBody emergencyNumber,
                                             @Part("password") RequestBody password, @Part("deviceid") RequestBody deviceid, @Part("role") RequestBody role,
                                             @Part("registerFrom") RequestBody registerFrom, @Part("AppId") RequestBody AppId,
                                             @Part MultipartBody.Part filename);
}
