package com.example.myapplication.model

// 알림 서비스 전용 DTO 현재 기능 OFF
data class PushDTO(var to: String? = null,
                   var notification: Notification? = Notification()) {
    data class Notification(var body: String? = null,
                            var title: String? = null)
}