package com.example.bankaccounts

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.security.MessageDigest


class MainActivity : AppCompatActivity() {
    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
    private external fun getHashedPwd(): String

    private lateinit var userID: EditText
    private lateinit var masterkey: EditText
    private lateinit var connectBtn: Button

    private val MASTERKEY: String = getHashedPwd()

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectBtn=findViewById(R.id.connectBtn)
        userID = findViewById(R.id.userID)
        masterkey = findViewById(R.id.masterkey)

        //We enable the click of the button whenever the user completed both ID and masterkey field
        connectBtn.isEnabled=false
        masterkey.addTextChangedListener(watcher)
        userID.addTextChangedListener(watcher)
    }

    //we need to define the TextWatcher object appart and not directly in the addTextChangedListener
    //as a lambda because our onTextChanged function needs to check both the UserId and masterkey
    //edit text in order to enable the connect button. If we didn't define this object here, we would
    //have had to implement the TextWatcher interface twice in the addTextChangedListener parameters,
    //once for userID and once for masterkey.
    private val watcher= object: TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            val inputID = userID.text.toString()
            val inputMasterkey = masterkey.text.toString()
            connectBtn.isEnabled=(inputID.isNotEmpty() && inputMasterkey.isNotEmpty())
        }
        override fun afterTextChanged(s: Editable) {}
    }

    override fun onStart(){
        super.onStart()

        connectBtn = findViewById(R.id.connectBtn)
        userID = findViewById(R.id.userID)
        masterkey = findViewById(R.id.masterkey)

        //This checks if the masterkey and the id of the user are good.
        connectBtn.setOnClickListener {
            val inputID = userID.text.toString().toInt()
            val inputMasterkey = masterkey.text.toString()
            if (hashPassword(inputMasterkey)==polyDecryption(MASTERKEY,inputMasterkey) && (inputID in 1..73) ){
                val intent = Intent(this@MainActivity, accounts::class.java)
                intent.putExtra("ID", inputID)
                intent.putExtra("masterkey",inputMasterkey)
                startActivity(intent)
            }
            else{
                Toast.makeText(applicationContext, "Wrong inputs", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //This is the function that is used to check whether or not the masterkey given by the user is correct
    //We don't store the value of the masterkey in clear in the source code. We hashed it once and
    //then hid the hashed value somewhere else to make it harder for any hacker to steal it.
    // To see if the user inputed the correct masterkey, we first hash his input, then we check if
    // this hash value corresponds to the value of our hash masterkey.
    private fun hashPassword(pwd: String): String{
        //This function was also used to hash the masterkey the first time.
        //to do so, we simply replaced the parameter with the variable password filled with the
        //password we wanted to be the Masterkey giving access to the app.
        // --> val password: String = "Masterkey"
        val md = MessageDigest.getInstance("SHA-256")
        md.update(pwd.toByteArray())
        val byteData = md.digest()
        val hexString = StringBuffer()
        for (i in byteData.indices)
        {
            val hex = Integer.toHexString(0xff and byteData[i].toInt())
            if (hex.length == 1) hexString.append('0')
            hexString.append(hex)
        }
        return hexString.toString()
    }

    private fun polyDecryption(to_decrypt: String, key: String): String {
        val decrypted = StringBuilder()
        val size= key.length
        for(i in to_decrypt.indices){
            var oldAscii= to_decrypt[i].toInt() - key[i%size].toInt()
            if(oldAscii<32){
                val diff=32-oldAscii
                oldAscii=127-diff
            }
            decrypted.append(oldAscii.toChar())
        }
        return decrypted.toString()
    }
}