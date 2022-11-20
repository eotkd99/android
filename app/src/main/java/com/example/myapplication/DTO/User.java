package com.example.myapplication.DTO;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class User {
        @SerializedName("userId")
        @Expose
        private String userId;

        @SerializedName("id")
        @Expose
        private String id;

        @SerializedName("title")
        @Expose
        private String title;

        @SerializedName("body")
        @Expose
        private String body;


        public String getUserId(){
            return userId;
        }

        public String getID(){
            return id;
        }

        public String getTitle(){
            return title;
        }

        public String getBody(){
            return body;
        }
}
