package com.example.myapplication.navigation

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.myapplication.LoginActivity
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.model.AlarmDTO
import com.example.myapplication.model.ContentDTO
import com.example.myapplication.model.FollowDTO
import com.example.myapplication.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_alarm.view.*
import kotlinx.android.synthetic.main.fragment_user.view.*
import java.util.*

class UserFragment : Fragment(){
    var fragmentView : View? = null
    var firestore : FirebaseFirestore? = null
    var uid : String? = null
    var auth : FirebaseAuth? = null
    var currentUserUid : String? = null
    companion object {
        var PICK_PROFILE_FROM_ALBUM = 10
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // 유저 프래그먼트 인플레이트
        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_user,container,false)
        // 다른 유저가 마이페이지 접근 시 동일한 유저인지 검사하기 위함
        uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid
        //현재 사용자의 UID와 해당 마이페이지에 접근한 사용자의 UID가 같으면
        if(uid == currentUserUid){
            //버튼의 문자를 로그아웃으로 변경
            fragmentView?.account_btn_follow_signout?.text = getString(R.string.signout)
            //버튼 클릭 이벤트 리스너
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                //엑티비티 종료
                activity?.finish()
                //로그인 페이지 인텐트
                startActivity(Intent(activity,LoginActivity::class.java))
                //auth 로그아웃
                auth?.signOut()
            }
        }else{ //현재 사용자의 UID와 해당 마이페이지에 접근한 사용자의 UID가 다르면
            // 버튼 문자를 팔로우로 변경
            fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
            // 메인 엑티비티 가져오기
            var mainactivity = (activity as MainActivity)
            // 상단 툴바에 유저 이름 할당
            mainactivity?.toolbar_username?.text = arguments?.getString("userId")
            // 돌아가기 버튼 이벤트 리스너
            mainactivity?.toolbar_btn_back?.setOnClickListener {
                // 클릭 시 네비게이션 바 값을 홈으로 바꿔 이동시키기
                mainactivity.bottom_navigation.selectedItemId = R.id.action_home
            }
            // 로고 숨기기
            mainactivity?.toolbar_title_image?.visibility = View.GONE
            // 상단 툴바에 유저이름, 돌아가기 버튼 보이기
            mainactivity?.toolbar_username?.visibility = View.VISIBLE
            mainactivity?.toolbar_btn_back?.visibility = View.VISIBLE
            // 팔로우 버튼 클릭 시 기능 수행
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                requestFollow()
            }
        }
        // 어댑터, 레이아웃 매니저 할당
        fragmentView?.account_reyclerview?.adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.account_reyclerview?.layoutManager = GridLayoutManager(requireActivity(), 3)
        // 프로필 클릭 시 이벤트 리스너
        fragmentView?.account_iv_profile?.setOnClickListener {
            //디바이스 앨범 열기
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent,PICK_PROFILE_FROM_ALBUM)
        }
        getProfileImage() //이미지 불러오기
        getFollowerAndFollowing() // 팔로우, 팔로잉 불러오기
        return fragmentView
    }
    // 팔로우, 팔로잉 불러오기 
    fun getFollowerAndFollowing(){
        //유저컬렉션에서 유저 UID로 필터링 후 가져옴 (내 페이지 클릭 = 내 UID, 상대방 페이지 클릭 = 상대방 UID)
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null) return@addSnapshotListener
            //스냅샷으로 실시간 불러와 캐스팅 후 할당
            var followDTO = documentSnapshot.toObject(FollowDTO::class.java)
            if(followDTO?.followerCount != null){
                fragmentView?.account_tv_follower_count?.text = followDTO?.followerCount?.toString()
                // UID가 현재 유저 UID 라면
                if(followDTO?.followers?.containsKey(currentUserUid!!)){
                    // 텍스트, 배경 설정
                    fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow_cancel)
                    fragmentView?.account_btn_follow_signout?.background
                        ?.setColorFilter(
                            ContextCompat.getColor(requireActivity(),R.color.colorLightGray),
                            PorterDuff.Mode.MULTIPLY)
                }else{// UID가 상대방 UID 라면
                    if(uid != currentUserUid){
                        // 텍스트, 배경 설정
                        fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
                        fragmentView?.account_btn_follow_signout?.background?.colorFilter = null
                    }

                }
            }
        }
    }
    // 팔로우 하는 기능
    fun requestFollow(){
        // 현재 사용자 마이페이지의 팔로우 팔로잉 수
        var tsDocFollowing = firestore?.collection("users")?.document(currentUserUid!!)
        // 기존의 팔로우 데이터를 가져옴
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            // null 이면 새로 생성
            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followingCount = 1
                followDTO!!.followings[uid!!] = true
                transaction.set(tsDocFollowing,followDTO!!)
                return@runTransaction
            }
            //상대방이 나를 팔로우 하는 중일 때 취소
            if(followDTO.followings.containsKey(uid)){
                followDTO?.followingCount = followDTO?.followingCount - 1
                followDTO?.followings.remove(uid)
            }else{
                //상대방이 나를 팔로우 안하는 중일 때 하게 됨
                followDTO?.followingCount = followDTO?.followingCount + 1
                followDTO?.followings[uid!!] = true
            }
            transaction.set(tsDocFollowing,followDTO)
            return@runTransaction
        }
        // 내가 접속한 상대방 마이페이지의 팔로우 팔로잉 수
        var tsDocFollower = firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)
                transaction.set(tsDocFollower,followDTO!!)
                return@runTransaction
            }
            //상대방을 팔로우 하는 것을 취소
            if(followDTO!!.followers.containsKey(currentUserUid!!)){
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid!!)
            }else{
                //상대방을 팔로우 하게 됨
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)
            }
            transaction.set(tsDocFollower,followDTO!!)
            return@runTransaction
        }
    }
    // 팔로워 알람 기능
    fun followerAlarm(destinationUid : String){
        var alarmDTO = AlarmDTO()
        // 알람 데이터 할당
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth?.currentUser?.email
        alarmDTO.uid = auth?.currentUser?.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp = System.currentTimeMillis()
        // 알람 컬렉션에 저장
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
        // 메시지 작성 후 알림전송 (디바이스 알림 OFF 중)
        var message = auth?.currentUser?.email + getString(R.string.alarm_follow)
        FcmPush.instance.sendMessage(destinationUid,"EveryDay",message)
    }
    // 프로필 사진 가져오기 기능
    fun getProfileImage(){
        //profileImages 컬렉션에서 현재 유저의 UID로 필터링 하여 가져옴
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null) return@addSnapshotListener
            //리스트에 저장
            if(documentSnapshot.data != null){
                var url = documentSnapshot?.data!!["image"]
                // 글라이드로 동적 할당
                Glide.with(requireActivity()).load(url).apply(RequestOptions().circleCrop()).into(fragmentView?.account_iv_profile!!)
            }
        }
    }
    // 어댑터
    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        // 게시글 리스트
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        init {
            // 파이어베이스 접속, 이미지 컬렉션 에서 마이페이지 소유자의 UID로 필터링 하여 사진데이터 가져옴
            firestore?.collection("images")?.whereEqualTo("uid",uid)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if(querySnapshot == null) return@addSnapshotListener
                //리스트에 데이터 받기
                for(snapshot in querySnapshot.documents){
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
                // 게시글 수 할당
                fragmentView?.account_tv_post_count?.text = contentDTOs.size.toString()
                // 리사이클 뷰 새로고침
                notifyDataSetChanged()
            }
        }
        // 뷰 홀더 생성
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            // 3x3 격자모양
            var width = resources.displayMetrics.widthPixels / 3
            var imageview = ImageView(p0.context)
            // 정사각형 크기로 인자값 할당
            imageview.layoutParams = LinearLayoutCompat.LayoutParams(width,width)
            // 커스텀뷰 인자로 넣어서 반환
            return CustomViewHolder(imageview)
        }

        inner class CustomViewHolder(var imageview: ImageView) : RecyclerView.ViewHolder(imageview) {}

        // 리스트 사이즈 반환
        override fun getItemCount(): Int {
            return contentDTOs.size
        }
        // 뷰 홀더 바인딩
        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
            // 커스텀 뷰 홀더로 캐스팅 후 이미지 받아옴
            var imageview = (p0 as CustomViewHolder).imageview
            // 글라이드로 이미지 동적 할당
            Glide.with(p0.itemView.context).load(contentDTOs[p1].imageUrl).apply(RequestOptions().centerCrop()).into(imageview)
        }

    }
}