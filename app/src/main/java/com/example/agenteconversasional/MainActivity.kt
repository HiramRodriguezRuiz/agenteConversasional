package com.example.agenteconversasional

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    val API_KEY = "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"
    val url  = "https://api.openai.com/v1/completions"
    lateinit var recyclerView: RecyclerView
    lateinit var welcomeText :TextView
    lateinit var messageEditText:EditText
    lateinit var sendButton:ImageButton
    lateinit var messageList:MutableList<Message>
    lateinit var messageAdapter:MessageAdapter
    val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .build()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        messageList = ArrayList()
        recyclerView = findViewById(R.id.recycler_view)
        welcomeText = findViewById(R.id.welcome_text)
        messageEditText = findViewById(R.id.message_edit_text)
        sendButton = findViewById(R.id.send_bt)
        messageAdapter = MessageAdapter(messageList)
        recyclerView.adapter = messageAdapter
        val layoutManger = LinearLayoutManager(this)
        layoutManger.stackFromEnd = true
        recyclerView.layoutManager = layoutManger

        sendButton.setOnClickListener {
            val question = messageEditText.text.toString().trim{ it <= ' '}
            addToChat(question,Message.SENT_BY_ME)
            messageEditText.setText("")
            callAPI(question)
            welcomeText.visibility = View.GONE
        }
    }

    private fun addToChat(message: String, sentBy: String) {
        runOnUiThread{
            messageList.add(Message(message,sentBy))
            messageAdapter.notifyDataSetChanged()
            recyclerView.smoothScrollToPosition(messageAdapter.itemCount)
        }

    }

    fun addResponse(response:String?){
        messageList.removeAt(messageList.size -1)
        addToChat(response!!,Message.SENT_BY_BOT)

    }

    private fun callAPI(question: String) {
        //call okhttp
        messageList.add(Message("Typing...",Message.SENT_BY_BOT))
        val jsonBody = JSONObject()
        /**
            val requestBody="""
            {
            "model": "gpt-3.5-turbo",
            "messages": [{
                "role": "user",
                "content": "$question"
            }],
            "max_tokens": 500,
            "temperature": 0
            }
            """.trimIndent()
        **/
        try {
            jsonBody.put("model", "gpt-3.5-turbo")
            jsonBody.put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", question)))
            jsonBody.put("max_tokens", 4000)
            jsonBody.put("temperature", 0)
        }catch (e:JSONException){
            e.printStackTrace()
        }

        val body :RequestBody = RequestBody.create(JSON,jsonBody.toString())
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $API_KEY")
            .post(body)
            .build()
        client.newCall(request).enqueue(object :Callback{
            override fun onFailure(call: Call, e: IOException) {
                addResponse("(Call API)Failed to load response due to ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body=response.body?.string()
                if (response.isSuccessful) {
                    if (body != null) {
                        Log.v("data",body.toString())
                    }

                    try {
                        /**jsonObject = JSONObject(response.body!!.string())**/
                        val jsonObject= JSONObject(body)
                        val jsonArray = jsonObject.getJSONArray("choices")
                        val result = jsonArray.getJSONObject(0).getString("text")
                        addResponse(result.trim())
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }else{
                    addResponse("(Message)Failed to load response due to ${response.body.toString()}")
                    Log.v("data","Body Empty")

                }
            }

        })

    }
    companion object{
        val JSON :MediaType = "application/json".toMediaType()
    }
}