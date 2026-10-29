package com.lcn.msmssync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var tvUserStatus: TextView
    private lateinit var btnGoogleSignIn: Button
    private lateinit var listView: ListView
    private lateinit var btnSyncManual: Button
    private lateinit var switchShare: Switch
    
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    private val smsList = ArrayList<String>()
    private val smsFullContent = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvUserStatus = findViewById(R.id.tvUserStatus)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        listView = findViewById(R.id.listViewSms)
        btnSyncManual = findViewById(R.id.btnSyncManual)
        switchShare = findViewById(R.id.switchShare)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, smsList)
        listView.adapter = adapter

        val sharedPrefs = getSharedPreferences("SyncPrefs", Context.MODE_PRIVATE)
        switchShare.isChecked = sharedPrefs.getBoolean("isSharingEnabled", true)
        switchShare.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("isSharingEnabled", isChecked).apply()
        }

        updateUIForUser()

        btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        btnSyncManual.setOnClickListener {
            if (auth.currentUser == null) {
                Toast.makeText(this, "Hãy đăng nhập tài khoản Google!", Toast.LENGTH_SHORT).show()
            } else {
                SyncManager.syncLocalSmsToCloud(this)
                Toast.makeText(this, "Đang chạy đồng bộ thủ công...", Toast.LENGTH_SHORT).show()
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            AlertDialog.Builder(this)
                .setTitle("Nội dung chi tiết SMS")
                .setMessage(smsFullContent[position])
                .setPositiveButton("Đóng", null)
                .show()
        }

        checkSmsPermissions()
    }

    private fun checkSmsPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS), 101)
        } else {
            startListeningCloudSms()
        }
    }

    private fun updateUIForUser() {
        val user = auth.currentUser
        if (user != null) {
            tvUserStatus.text = "Tài khoản: ${user.email}"
            btnGoogleSignIn.text = "Đăng xuất"
            btnGoogleSignIn.setOnClickListener {
                auth.signOut()
                updateUIForUser()
                smsList.clear()
                smsFullContent.clear()
                adapter.notifyDataSetChanged()
            }
            startListeningCloudSms()
        } else {
            tvUserStatus.text = "Chưa kết nối tài khoản"
            btnGoogleSignIn.text = "Đăng nhập Google"
            btnGoogleSignIn.setOnClickListener { signInWithGoogle() }
        }
    }

    private fun signInWithGoogle() {
        val credentialManager = CredentialManager.create(this)
        val serverClientId = "728642804006-kcn3hc5vbad7tb6j0208f9ddsj6hohtv.apps.googleusercontent.com" 

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@MainActivity, request)
                val credential = result.credential
                
                if (credential is GoogleIdTokenCredential) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                    auth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this@MainActivity, "Đăng nhập Google thành công!", Toast.LENGTH_SHORT).show()
                                updateUIForUser()
                            } else {
                                Toast.makeText(this@MainActivity, "Lỗi Firebase: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startListeningCloudSms() {
        val currentUser = auth.currentUser ?: return
        
        db.collection("users").document(currentUser.uid)
          .collection("messages")
          .orderBy("timestamp", Query.Direction.DESCENDING)
          .addSnapshotListener { snapshots, _ ->
              if (snapshots != null) {
                  smsList.clear()
                  smsFullContent.clear()
                  for (doc in snapshots) {
                      val address = doc.getString("address") ?: "Không rõ"
                      val body = doc.getString("body") ?: ""
                      val device = doc.getString("deviceId") ?: "Thiết Thi bị ẩn danh"
                      
                      smsList.add("SĐT: $address \n[Gửi từ: $device]")
                      smsFullContent.add("Người gửi: $address\nThiết bị: $device\n\nNội dung SMS:\n$body")
                  }
                  adapter.notifyDataSetChanged()
              }
          }
    }
}
