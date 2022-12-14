package com.example.myapplication.model
import java.util.HashMap

data class FollowDTO(
        var followerCount: Int = 0,
        var followers: MutableMap<String, Boolean> = HashMap(), //중복 방지

        var followingCount: Int = 0,
        var followings: MutableMap<String, Boolean> = HashMap() //중복 방지
)