package com.example.myapplication;

import android.app.Activity;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;

import com.example.myapplication.DTO.User;

import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;


public class loginAct extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Retrofit_interface retrofit_if = retrofit.create(Retrofit_interface.class);

        Call<User> call = retrofit_if.getUser("uri적기");

        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                User u = response.body();
                Log.d(Tag,"성공");
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.d(Tag,"실패");
            }
        });
    }
}
