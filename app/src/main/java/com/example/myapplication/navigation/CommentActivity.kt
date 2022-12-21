package com.example.myapplication.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.myapplication.R
import com.example.myapplication.model.*
import com.example.myapplication.util.FcmPush
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.item_comment.view.*

class CommentActivity : AppCompatActivity() {
    var contentUid : String? = null
    var destinationUid : String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)
        //메인화면에서 넘어올 때 데이터 받기
        contentUid = intent.getStringExtra("contentUid")
        destinationUid = intent.getStringExtra("destinationUid")

        //어댑터, 레이아웃 매니저 생성
        comment_recyclerview.adapter = CommentRecyclerviewAdapter()
        comment_recyclerview.layoutManager = LinearLayoutManager(this)

        //댓글 작성 이벤트 리스너
        comment_btn_send?.setOnClickListener {
            var comment = ContentDTO.Comment()
            // 댓글 객체 생성 후 현재 사용자 정보 저장
            comment.userId = FirebaseAuth.getInstance().currentUser?.email
            comment.uid = FirebaseAuth.getInstance().currentUser?.uid
            comment.comment = comment_edit_message.text.toString()
            comment.timestamp = System.currentTimeMillis()
            //파이어베이스 접속, 이미지컬랙션 속 댓글도큐먼트 접근 후 저장
            FirebaseFirestore.getInstance().collection("images").document(contentUid!!).collection("comments").document().set(comment)
            //알림 받기
            commentAlarm(destinationUid!!,comment_edit_message.text.toString())
            comment_edit_message.setText("") // 보내는 텍스트 필드 초기화
        }
    }
    //알림 받기 ( 기능 OFF 중 )
    fun commentAlarm(destinationUid : String, message : String){
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
        alarmDTO.kind = 1
        alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
        alarmDTO.timestamp = System.currentTimeMillis()
        alarmDTO.message = message
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        var msg = FirebaseAuth.getInstance().currentUser?.email + " " + getString(R.string.alarm_comment) + " of " + message
        FcmPush.instance.sendMessage(destinationUid,"EveryDay",msg)
    }
    // 어댑터
    inner class CommentRecyclerviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        // 댓글 리스트 생성
        var comments : ArrayList<ContentDTO.Comment> = arrayListOf()
        init {
            //파이어베이스 접속 후 이미지컬렉션 -> 댓글도큐먼트 에서 시간순서대로 읽어옴
            FirebaseFirestore.getInstance()
                .collection("images")
                .document(contentUid!!)
                .collection("comments")
                .orderBy("timestamp")
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    comments.clear() //중복 문제 방지 
                    if(querySnapshot == null)return@addSnapshotListener
                    //읽어오는 부분
                    for(snapshot in querySnapshot.documents!!){
                        comments.add(snapshot.toObject(ContentDTO.Comment::class.java)!!)
                    }
                    notifyDataSetChanged() // 리사이클 뷰 새로고침
                }
        }
        // 뷰 홀더 생성
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(p0.context).inflate(R.layout.item_comment,p0,false)
            return CustomViewHolder(view)
        }

        private inner class CustomViewHolder(view : View) : RecyclerView.ViewHolder(view)
        // 사이즈 반환
        override fun getItemCount(): Int {
            return comments.size
        }
        // 뷰 올더 바인딩
        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
            var view = p0.itemView
            // 인덱스 넣어서 맵핑하기
            view.commentviewitem_textview_comment.text = comments[p1].comment
            view.commentviewitem_textview_profile.text = comments[p1].userId
            // 프로필이미지컬렉션 -> 도큐먼트에서 이미지 읽어오기
            FirebaseFirestore.getInstance()
                .collection("profileImages")
                .document(comments[p1].uid!!)
                .get()
                .addOnCompleteListener { task ->
                    if(task.isSuccessful){ // 성공적으로 받아왔을 때 이미지 URL 저장
                        var url = task.result!!["image"]
                        // 원형으로 이미지 동적 할당
                        Glide.with(p0.itemView.context).load(url).apply(RequestOptions().circleCrop()).into(view.commentviewitem_imageview_profile)
                    }
                }
        }

    }
}
