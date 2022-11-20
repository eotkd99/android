package com.example.myapplication;

import com.example.myapplication.DTO.User;


import retrofit2.Call;
import retrofit2.http.*;

public interface Retrofit_interface {
    @GET("posts/{UserID}")
    Call<User> getUser(@Path("UserID") String userid);

    @POST("posts/{UserID}")
    Call<User> postUser(@Path("UserID") String userid);

    @DELETE("posts/{UserID}")
    Call<User> deleteUser(@Path("UserID") String userid);

    @PUT("posts/{UserID}")
    Call<User> putUser(@Path("UserID") String userid);
}
