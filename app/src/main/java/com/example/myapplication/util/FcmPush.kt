package com.example.myapplication.util

import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.example.myapplication.model.PushDTO
import okhttp3.*
import java.io.IOException

class  FcmPush {    //팔로우, 졸아요 알림 발생 시 해당 사용자에게 알림전송 ( 기능 Off 중 )

    var JSON = MediaType.parse("application/json; charset=utf-8")
    var url = "https://fcm.googleapis.com/fcm/send"
    var serverKey = "AIzaSyByKuzd1UI7RyK-r6eJdNfH-c_9FF6Ff4w"
    var gson : Gson? = null
    var okHttpClient : OkHttpClient? = null
    companion object{
        var instance = FcmPush()
    }

    init { // 초기화
        gson = Gson()
        okHttpClient = OkHttpClient()
    }
    // 알림 전송 함수
    fun sendMessage(destinationUid : String, title : String, message : String){
        FirebaseFirestore.getInstance().collection("pushtokens").document(destinationUid).get().addOnCompleteListener {
            task ->
            // 정상 실행
            if(task.isSuccessful){
                var token = task?.result?.get("pushToken").toString()
                // 값 담기
                var pushDTO = PushDTO()
                pushDTO.to = token
                pushDTO.notification?.title = title
                pushDTO.notification?.body = message
                // Json 형식 RequestBody 생성 후 담기
                var body = RequestBody.create(JSON,gson?.toJson(pushDTO))
                var request = Request.Builder()
                    .addHeader("Content-Type","application/json")
                    .addHeader("Authorization","key="+serverKey)
                    .url(url)
                    .post(body)
                    .build()
                // http 통신
                okHttpClient?.newCall(request)?.enqueue(object : Callback{
                    //실패
                    override fun onFailure(call: Call?, e: IOException?) { 
                    }
                    //성공
                    override fun onResponse(call: Call?, response: Response?) {
                        println(response?.body()?.string())
                    }

                })
            }
        }
    }
}