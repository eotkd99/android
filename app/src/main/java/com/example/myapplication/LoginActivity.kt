package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.*
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.*
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.*
import com.google.firebase.auth.*
import kotlinx.android.synthetic.main.activity_login.*
import java.security.*
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import java.util.*


class LoginActivity : AppCompatActivity() {
    var auth : FirebaseAuth? = null
    var googleSignInClient : GoogleSignInClient? = null
    var GOOGLE_LOGIN_CODE = 9001 // 로그인 할 때 사용할 응답코드
    var callbackManager : CallbackManager? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        // Firebase 인트턴스 할당
        auth = FirebaseAuth.getInstance()
        // 각 버튼 이벤트 리스너
        email_login_button.setOnClickListener {
            signinAndSignup()
        }
        google_sign_in_button.setOnClickListener {
            googleLogin()
        }
        facebook_login_button.setOnClickListener {
            facebookLogin()
        }
        // 빌더 패턴으로 로그인 옵션 생성 토큰 값, 이메일 담아서 할당
        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        // 옵션 값 세팅
        googleSignInClient = GoogleSignIn.getClient(this,gso)
        // 페이스북 콜백 매니저
        callbackManager = CallbackManager.Factory.create()
    }
    // 자동 로그인 기능
    override fun onStart() {
        super.onStart()
        moveMainPage(auth?.currentUser)
    }
    // 구글 로그인 처리
    fun googleLogin(){
        var signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent,GOOGLE_LOGIN_CODE)
    }
    // 페이스북 로그인 처리 (현재 유효하지 않은 ID라 기능 OFF 중)
    fun facebookLogin(){
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile","email"))
        LoginManager.getInstance()
            .registerCallback(callbackManager, object : FacebookCallback<LoginResult>{
                override fun onSuccess(result: LoginResult?) {
                    handleFacebookAccessToken(result?.accessToken)
                }
                override fun onCancel() {}
                override fun onError(error: FacebookException?) {}
            })
    }
    fun handleFacebookAccessToken(token : AccessToken?){
        var credential = FacebookAuthProvider.getCredential(token?.token!!)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener { task ->
                if(task.isSuccessful){
                    moveMainPage(task.result?.user)
                }else{
                    Toast.makeText(this,task.exception?.message,Toast.LENGTH_LONG).show()
                }
            }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager?.onActivityResult(requestCode,resultCode,data)
        // 엑티비티 돌아왔을 때 requestCode = 9001 이면
        if(requestCode == GOOGLE_LOGIN_CODE){
            // 로그인 결과 값 받기
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data!!)
            // 성공 시
            if(result!!.isSuccess){
                var account = result.signInAccount
                // 파이어베이스 접속 후  계정 생성
                firebaseAuthWithGoogle(account)
            }
        }
    }
    // 파이어베이스 접속 후 계정 생성
    fun firebaseAuthWithGoogle(account : GoogleSignInAccount?){
        // 토큰 값 할당  
        var credential = GoogleAuthProvider.getCredential(account?.idToken,null)
        // 로그인 시도
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener {
                    task ->
                if(task.isSuccessful){ //로그인 성공
                    moveMainPage(task.result?.user) // 메인으로 이동
                }else{ //실패
                    Toast.makeText(this,task.exception?.message,Toast.LENGTH_LONG).show()
                }
            }
    }
    //로그인 + 회원가입
    fun signinAndSignup(){
        // 파이어베이스 auth에 이메일, 비밀번호로 계정 생성
        auth?.createUserWithEmailAndPassword(email_edittext.text.toString(),password_edittext.text.toString())
            ?.addOnCompleteListener {
                    task ->
                if(task.isSuccessful){ //생성 성공
                    moveMainPage(task.result?.user)
                }else if(task.exception?.message.isNullOrEmpty()){ // 문법오류
                    Toast.makeText(this,task.exception?.message,Toast.LENGTH_LONG).show()
                }else{ //기존에 존재하는 이메일
                    signinEmail()
                }
            }
    }
    // 이메일 로그인
    fun signinEmail(){
        // 로그인 시도
        auth?.signInWithEmailAndPassword(email_edittext.text.toString(),password_edittext.text.toString())
            ?.addOnCompleteListener {
                    task ->
                if(task.isSuccessful){ //성공 시 이동
                    moveMainPage(task.result?.user)
                }else{ //오류
                    Toast.makeText(this,task.exception?.message,Toast.LENGTH_LONG).show()
                }
            }
    }
    //메인 이동
    fun moveMainPage(user:FirebaseUser?){
        if(user != null){
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }
    }
}