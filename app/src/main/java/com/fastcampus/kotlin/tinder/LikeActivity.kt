package com.fastcampus.kotlin.tinder

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.Direction

class LikeActivity : AppCompatActivity(), CardStackListener {

    private lateinit var auth : FirebaseAuth
    private lateinit var userDB : DatabaseReference
    private val adapter = CardItemAdapter()
    private val cardItems = mutableListOf<CardItem>()

    private val manager by lazy {
        CardStackLayoutManager(this, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_like)

        auth = Firebase.auth
        userDB = Firebase.database.reference.child("Users")
        val currentUserDB = userDB.child(getCurrentUserId())
        currentUserDB.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.child("name").value == null) {
                    showNameInputPopup()
                    return
                }
                getUnselectedUser();
            }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })

        initCardStackView()
        initLogoutButton()
        initMatchListButton()
    }

    private fun initLogoutButton() {
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun initMatchListButton() {
        val matchListButton = findViewById<Button>(R.id.matchListButton)
        matchListButton.setOnClickListener {
            startActivity(Intent(this, MatchedUserActivity::class.java))
        }
    }

    private fun initCardStackView() {
        val stackView = findViewById<CardStackView>(R.id.cardStackView)
        stackView.layoutManager = manager
        stackView.adapter = adapter
    }

    private fun getCurrentUserId() : String {
        if (auth.currentUser == null) {
            Toast.makeText(this, "로그인이 되어있지 않습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
        return auth.currentUser?.uid.orEmpty()
    }

    private fun getUnselectedUser() {
        userDB.addChildEventListener(object : ChildEventListener {
            @SuppressLint("NotifyDataSetChanged")
            //초기에 데이터를 불러오는 경우 및 새로운 유저가 저장되었을 경우
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (snapshot.child("userId").value != getCurrentUserId()
                    && snapshot.child("likedBy").child("like").hasChild(getCurrentUserId()).not()
                    && snapshot.child("likedBy").child("disLike").hasChild(getCurrentUserId()).not()) {

                    val userId = snapshot.child("userId").value.toString()
                    var name = "undecided"
                    if (snapshot.child("userId").value != null)
                        name = snapshot.child("userId").value.toString()

                    cardItems.add(CardItem(userId, name))
                    adapter.submitList(cardItems)
                    adapter.notifyDataSetChanged()
                }
            }

            //유저의 이름이 변경되었거나 유저가 다른 유저를 like 했을 경우
            @SuppressLint("NotifyDataSetChanged")
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                cardItems.find { it.name ==  snapshot.key}?.let { //변경된 유저 이름 카드 아이템 리스트에서 찾기
                    it.name = snapshot.child("userId").value.toString()
                }
                adapter.submitList(cardItems)
                adapter.notifyDataSetChanged()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                TODO("Not yet implemented")
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun showNameInputPopup() {
        var editText = EditText(this)

        AlertDialog.Builder(this)
            .setTitle("이름을 입력해 주세요.")
            .setView(editText)
            .setPositiveButton("저장") { _,_ ->
                if (editText.text.isEmpty())
                    showNameInputPopup()
                else
                    saveUserName(editText.text.toString())
            }
            .setCancelable(false)
            .show()
    }

    private fun saveUserName(name: String) {
        var userId = getCurrentUserId()
        var currentUserDB = userDB.child(userId)
        var user = mutableMapOf<String, Any>()
        user["userId"] = userId
        user["name"] = name
        currentUserDB.updateChildren(user)

        getUnselectedUser();
    }

    private fun like() {
        val card = cardItems[manager.topPosition -1]
        cardItems.removeFirst()

        userDB.child(card.userId).child("likedBy").child("like").child(getCurrentUserId()).setValue(true)

        saveMatchIgOtherLikeMe(card.userId)

        Toast.makeText(this, "${card.name}님을 like 하셨습니다", Toast.LENGTH_SHORT).show()
    }

    private fun disLike() {
        val card = cardItems[manager.topPosition -1]
        cardItems.removeFirst()

        userDB.child(card.userId).child("likedBy").child("disLike").child(getCurrentUserId()).setValue(true)

        Toast.makeText(this, "${card.name}님을 dislike 하셨습니다", Toast.LENGTH_SHORT).show()
    }

    private fun saveMatchIgOtherLikeMe(otherUserId : String) {
        val isOtherLikeME = userDB.child(getCurrentUserId()).child("likedBy").child("like").child(otherUserId)
        isOtherLikeME.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value == true) {
                    userDB.child(getCurrentUserId()).child("likedBy").child("match").child(otherUserId).setValue(true)
                    userDB.child(otherUserId).child("likedBy").child("match").child(getCurrentUserId()).setValue(true)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    override fun onCardDragging(direction: Direction?, ratio: Float) {}

    override fun onCardSwiped(direction: Direction?) {
        when (direction) {
            Direction.Right -> like()
            Direction.Left -> disLike()
        }
    }

    override fun onCardRewound() {}

    override fun onCardCanceled() {}

    override fun onCardAppeared(view: View?, position: Int) {}

    override fun onCardDisappeared(view: View?, position: Int) {}


}