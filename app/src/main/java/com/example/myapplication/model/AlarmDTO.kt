package com.example.myapplication.model

data class AlarmDTO (
        var destinationUid: String? = null, // 다른 유저가 접근할 때 UID
        var userId: String? = null, // 현재 유저 아이디
        var uid: String? = null, // 현재 유저 UID
        var kind: Int = 0, //0 : 좋아요, 1: 메세지, 2: 팔로우
        var message: String? = null,
        var timestamp: Long? = null
)