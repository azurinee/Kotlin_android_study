package com.fastcampus.kotlin.tinder

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fastcampus.kotlin.tinder.DBKey.Companion.LIKED_BY
import com.fastcampus.kotlin.tinder.DBKey.Companion.MATCH
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MatchedUserActivity : AppCompatActivity() {

    private lateinit var auth : FirebaseAuth
    private lateinit var userDB : DatabaseReference
    private val adapter = MatchedUserAdapter()
    private val cardItems = mutableListOf<CardItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activiy_match)

        auth = Firebase.auth
        userDB = FirebaseDatabase.getInstance().reference.child("Users")

        initMatchedRecyclerView()
        getMatchedUsers()
    }

    private fun initMatchedRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.matchedUserRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun getMatchedUsers() {
        val matchedUserDB = userDB.child(getCurrentUserId()).child(LIKED_BY).child(MATCH)

        matchedUserDB.addChildEventListener(object: ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (snapshot.key?.isNotEmpty() == true) {
                    getMatchUser(snapshot.key.orEmpty())
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) { }

            override fun onChildRemoved(snapshot: DataSnapshot) { }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) { }

            override fun onCancelled(error: DatabaseError) { }

        })
    }

    private fun getMatchUser(userId: String) {
        val matchedDB = userDB.child(userId)
        matchedDB.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cardItems.add(CardItem(userId, snapshot.child("name").value.toString()))
                adapter.submitList(cardItems)
            }
            override fun onCancelled(error: DatabaseError) { }
        })
    }

    private fun getCurrentUserId() : String {
        if (auth.currentUser == null) {
            Toast.makeText(this, "로그인이 되어있지 않습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
        return auth.currentUser?.uid.orEmpty()
    }

}