package com.example.myapplication.navigation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.myapplication.R
import com.example.myapplication.model.*
import com.example.myapplication.util.FcmPush
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_comment.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailViewFragment : Fragment(){
    var firestore : FirebaseFirestore? = null
    var uid : String? = null
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //인플레이트
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_detail,container,false)
        //파이어베이스 객체 생성 후 현재 유저 UID 저장
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid
        //어댑터, 매니저 할당
        view.detailviewfragment_recyclerview.adapter = DetailViewRecyclerViewAdapter()
        view.detailviewfragment_recyclerview.layoutManager = LinearLayoutManager(activity)
        return view
    }
    // 어댑터
    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        // 게시글 리스트
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        // 유저 UID 리스트
        var contentUidList : ArrayList<String> = arrayListOf()
        init {
            // 파이어베이스 접속 이미지컬렉션에서 시간순으로 데이터 가져오기
            firestore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                // 중복 문제 방지
                contentDTOs.clear()
                contentUidList.clear()
                //Sometimes, This code return null of querySnapshot when it signout
                if(querySnapshot == null) return@addSnapshotListener
                // 각 데이터 저장
                for(snapshot in querySnapshot!!.documents){
                    var item = snapshot.toObject(ContentDTO::class.java)
                    contentDTOs.add(item!!)
                    contentUidList.add(snapshot.id)
                }
                notifyDataSetChanged() // 리사이클 뷰 새로고침
            }
        }
        // 뷰 홀더 생성
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(p0.context).inflate(R.layout.item_detail,p0,false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)
        // 게시글 사이즈 리턴
        override fun getItemCount(): Int {
            return contentDTOs.size
        }
        // 뷰 홀더 바인딩
        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
            var viewholder = (p0 as CustomViewHolder).itemView
            viewholder.detailviewitem_profile_textview.text = contentDTOs!![p1].userId //아이디
            Glide.with(p0.itemView.context).load(contentDTOs!![p1].imageUrl).into(viewholder.detailviewitem_imageview_content) //이미지 동적 할당
            viewholder.detailviewitem_explain_textview.text = contentDTOs!![p1].explain //내용
            viewholder.detailviewitem_favoritecounter_textview.text = "Likes " + contentDTOs!![p1].favoriteCount //좋아요
            // 좋아요 버튼 이벤트 리스너
            viewholder.detailviewitem_favorite_imageview.setOnClickListener {
                favoriteEvent(p1)
            }
            // 좋아요 데이터가 현재 유저 값 보유, 좋아요 누름 상태
            if(contentDTOs!![p1].favorites.containsKey(uid)){
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite) // 색칠된 하트
            }else{ // 누르지 않음
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_border) // 빈 하트
            }

            // 게시글 프로필 사진 클릭 이벤트 리스너
            viewholder.detailviewitem_profile_image.setOnClickListener {
                // 클릭한 유저의 마이페이지 이동 전 데이터 담기
                var fragment = UserFragment()
                // 번들을 사용, 클릭한 유저 UID, ID 담기
                var bundle = Bundle()
                bundle.putString("destinationUid",contentDTOs[p1].uid)
                bundle.putString("userId",contentDTOs[p1].userId)
                //인자로 저장
                fragment.arguments = bundle 
                //인자 넣고 플래그먼트 동적으로 변환
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content,fragment)?.commit()
            }
            // 댓글 아이콘 클릭 이벤트 리스너
            viewholder.detailviewitem_comment_imageview.setOnClickListener { v ->
                // 댓글 엑티비티 인텐트 후 게시글 UID, 소유자 UID 보내기
                var intent = Intent(v.context,CommentActivity::class.java)
                intent.putExtra("contentUid",contentUidList[p1])
                intent.putExtra("destinationUid",contentDTOs[p1].uid)
                // 엑티비티전환
                startActivity(intent)
            }
        }
        // 좋아요 버튼 처리 함수
        fun favoriteEvent(position : Int){
            // 리사이클 뷰에서 선택한 인덱스를 인자로 받아서 해당 UID값을 가져옴
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])
            // 저장소 데이터 처리 이전에 트랜잭션 생성 (현재 유저의 게시글 좋아요 유무를 판단하기 위함)
            firestore?.runTransaction { transaction ->
                // 해당 게시글 데이터 가져옴
                var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)
                // 데이터의 좋아요가 현재 유저의 UID값을 보유, 눌러져있는 상태
                if(contentDTO!!.favorites.containsKey(uid)){
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount - 1 // 좋아요 감소
                    contentDTO?.favorites.remove(uid) // 좋아요에서 현재 유저 UID 제거 (초기화)
                }else{ // 눌러지지 않은 상태
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount + 1 // 좋아요 증가
                    contentDTO?.favorites[uid!!] = true //현재 유저의 UID값 삽입
                    favoriteAlarm(contentDTOs[position].uid!!) // 알람 기능
                }
                transaction.set(tsDoc,contentDTO) //트랜잭션 반영
            }
        }
        // 좋아요 알림 기능 처리 함수
        fun favoriteAlarm(destinationUid : String){
            // 현재 유저 값 받아오기
            var alarmDTO = AlarmDTO()
            alarmDTO.destinationUid = destinationUid
            alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
            alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
            alarmDTO.kind = 0
            alarmDTO.timestamp = System.currentTimeMillis()
            // 파이어베이스 접속 후 알람컬렉션 -> 도큐먼트에 위의 값 저장
            FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
            // 현재 유저 이메일 값 받아오기
            var message = FirebaseAuth.getInstance()?.currentUser?.email + getString(R.string.alarm_favorite)
            // 디바이스로 알람 보내기 (기능 OFF 중)
            FcmPush.instance.sendMessage(destinationUid,"Howlstagram",message)
        }

    }
}