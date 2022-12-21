package com.example.myapplication.navigation

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.myapplication.R
import com.example.myapplication.model.*
import kotlinx.android.synthetic.main.fragment_alarm.view.*
import kotlinx.android.synthetic.main.item_comment.view.*

class AlarmFragment : Fragment(){
    // 알람 프래그먼트 인플레이트, 리사이클뷰 어댑터와 매니저 생성
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_alarm,container,false)
        view.alarmfragment_recyclerview.adapter = AlarmRecyclerviewAdapter()
        view.alarmfragment_recyclerview.layoutManager = LinearLayoutManager(activity)
        return view
    }
    //어댑터
    inner class AlarmRecyclerviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var alarmDTOList : ArrayList<AlarmDTO> = arrayListOf() // 알람 리스트 생성
        init {
            val uid = FirebaseAuth.getInstance().currentUser?.uid // 현재 사용자 UID
            //파이어베이스 접속, 알람 데이터 가져옴, 현재 유저에게 온 메세지만 필터링
            FirebaseFirestore.getInstance().collection("alarms").whereEqualTo("destinationUid",uid).addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                //중복 방지
                alarmDTOList.clear()
                if(querySnapshot == null) return@addSnapshotListener
                //읽은 값 리스트에 할당
                for(snapshot in querySnapshot.documents){ 
                    alarmDTOList.add(snapshot.toObject(AlarmDTO::class.java)!!)
                }
                notifyDataSetChanged() // 리사이클 뷰 새로고침
            }
        }
        //뷰 홀더 생성
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(p0.context).inflate(R.layout.item_comment,p0,false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view : View) : RecyclerView.ViewHolder(view)
        // 리스트 사이즈 반환
        override fun getItemCount(): Int {
            return alarmDTOList.size
        }
        //뷰 홀더 바인딩
        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
            var view = p0.itemView
            // 파이어베이스 접속 후 알람 객체 가져오고 사진 URL 저장
            FirebaseFirestore.getInstance().collection("profileImages").document(alarmDTOList[p1].uid!!).get().addOnCompleteListener { task ->
                if(task.isSuccessful){
                    val url = task.result!!["image"]
                    Glide.with(view.context).load(url).apply(RequestOptions().circleCrop()).into(view.commentviewitem_imageview_profile)
                    //글라이드로 사진 동적으로 적용하기
                }
            }
            // 가져온 알람 데이터에서 kind 속성에 따라 문자열 할당
            when(alarmDTOList[p1].kind){ // kind 숫자 별 알림 설정 0=좋아요 1=메세지 2=팔로잉
                0 -> {
                    val str_0 = alarmDTOList[p1].userId + getString(R.string.alarm_favorite)
                    view.commentviewitem_textview_profile.text = str_0
                }
                1 -> {
                    val str_0 = alarmDTOList[p1].userId + " " + getString(R.string.alarm_comment) +" of " + alarmDTOList[p1].message
                    view.commentviewitem_textview_profile.text = str_0
                }
                2 -> {
                    val str_0 = alarmDTOList[p1].userId + " " + getString(R.string.alarm_follow)
                    view.commentviewitem_textview_profile.text = str_0
                }
            }
            //사진, 글자 잉여값 숨김
            view.commentviewitem_textview_comment.visibility = View.INVISIBLE
        }
    }
}