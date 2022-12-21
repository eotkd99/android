package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.example.myapplication.navigation.*
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.*
import kotlinx.android.synthetic.main.activity_main.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    //네비게이션 바 이벤트 리스너
    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        // 상대방이 내 페이지 열람시 보이는 위젯 숨김
        setToolbarDefault()
        when(p0.itemId){
            // 홈화면
            R.id.action_home ->{
                var detailViewFragment = DetailViewFragment() // 프래그먼트 할당
                // detailViewFragment로 전환
                supportFragmentManager.beginTransaction().replace(R.id.main_content,detailViewFragment).commit()
                return true
            }
            // 나와 상호작용한 게시글 + 내 게시글
            R.id.action_search ->{
                var gridFragment = GridFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content,gridFragment).commit()
                return true
            }
            // 사진 추가
            R.id.action_add_photo ->{
                //외부 저장소 읽기 권한 확인
                if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    startActivity(Intent(this,AddPhotoActivity::class.java))
                }
                return true
            }
            // 알림 기능
            R.id.action_favorite_alarm ->{
                var alarmFragment = AlarmFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content,alarmFragment).commit()
                return true
            }
            // 마이페이지
            R.id.action_account ->{
                var userFragment = UserFragment()
                var bundle = Bundle()
                //현재 사용자 UID 받아옴
                var uid = FirebaseAuth.getInstance().currentUser?.uid
                // 번들에 destinationUid로 넣음 (마이페이지가 현재 사용자의 것인지 확인을 위함)
                bundle.putString("destinationUid",uid)
                // 플래그먼트 인자로 번들 할당
                userFragment.arguments = bundle
                // 교체
                supportFragmentManager.beginTransaction().replace(R.id.main_content,userFragment).commit()
                return true
            }
        }
        return false
    }
    // 상대방이 내 페이지 열람시 보이는 위젯 숨김
    fun setToolbarDefault(){
        toolbar_username.visibility = View.GONE
        toolbar_btn_back.visibility = View.GONE
        toolbar_title_image.visibility = View.VISIBLE
    }
    // 디바이스 알림을 위한 토큰값 파이어베이스에 넣어주기 (현재 기능 OFF 중)
    fun registerPushToken(){
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (task.result != null && !TextUtils.isEmpty(task.result)) {
                        val token:String = task.result!!
                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        val map = mutableMapOf<String,Any>()
                        map["pushToken"] = token!!
                        FirebaseFirestore.getInstance().collection("pushtokens").document(uid!!).set(map)
                    }
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bottom_navigation.setOnNavigationItemSelectedListener(this)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),1)
        // 초기 화면 = 홈
        bottom_navigation.selectedItemId = R.id.action_home
        registerPushToken()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 유저 프래그먼트 프로필 사진 설정 기능 후 돌아왔을 때 사진선택 유무 + 응답코드 확인
        if(requestCode == UserFragment.PICK_PROFILE_FROM_ALBUM && resultCode == Activity.RESULT_OK){
            // 이미지 데이터 저장
            var imageUri = data?.data
            // 현재 유저 UID 획득
            var uid = FirebaseAuth.getInstance().currentUser?.uid
            // 저장소에 userProfileImages 경로 획득
            var storageRef = FirebaseStorage.getInstance().reference.child("userProfileImages").child(uid!!)
            // 저장소에 이미지 저장
            storageRef.putFile(imageUri!!).continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
                return@continueWithTask storageRef.downloadUrl
            }.addOnSuccessListener { uri -> //성공 시
                var map = HashMap<String,Any>()
                map["image"] = uri.toString()
                //파이어베이스 profileImages 컬렉션에 저장
                FirebaseFirestore.getInstance().collection("profileImages").document(uid).set(map)
            }
        }
    }
}