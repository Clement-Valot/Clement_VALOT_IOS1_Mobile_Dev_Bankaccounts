@file:Suppress("BlockingMethodInNonBlockingContext")

package com.example.bankaccounts

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class accounts : AppCompatActivity() {
    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
    private external fun getAPIkey(): String
    private external fun getEncryptionKey(): String
    private external fun getUserFile(): String
    private external fun getAccountFile(): String

    //private lateinit var refreshBtn: Button
    private lateinit var okBtn: Button
    private lateinit var accountID: EditText
    private lateinit var name: TextView
    private lateinit var nameAccount: TextView
    private lateinit var amount: TextView
    private lateinit var iban: TextView
    private lateinit var currency: TextView

    private var mk: String = "masterkey"
    //All those string are hidden and encrypted in the hidden file. So even if someone succeeds to
    //get to this hidden file he would have to decrypt what is inside of it and he would need the
    //masterkey to do so
    private val API_KEY : String = getAPIkey()
    private val ENCRYPTION_KEY : String = getEncryptionKey()
    private val FILE_NAME_USER : String = getUserFile()
    private val FILE_NAME_ACCOUNT : String = getAccountFile()

    //Value we give to the JSONObject in readFromFile function in case the client tries to access
    //info offline while he has never gotten them online. In order to make it a correct JSONObject,
    //we need to put it under this format
    private val JSONerror = JSONObject("{data:{'error':'error'}}")
    private val error = "Get info Online at least once before trying offline"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accounts)

        mk= intent.getStringExtra("masterkey").toString()
        //we can use these two lines instead of the next try catch to get the info but the network
        //connectivity is more subject to errors
        /*val info = if(CheckConnection()){ secureConnect(FILE_NAME_USER,"config")}
        else{ ReadFromFile(FILE_NAME_USER, applicationContext) }*/

        //We try to get information online and if the connection fails, then we get info from the file
        //in the internal storage of the device
        lateinit var info: JSONObject
        try{
            info=secureConnect(polyDecryption(FILE_NAME_USER,mk),"config")
            Toast.makeText(applicationContext, "Info got Online", Toast.LENGTH_SHORT).show()
        }
        catch(e: Exception){
            info=readFromFile(polyDecryption(FILE_NAME_USER,mk), applicationContext)
            Toast.makeText(applicationContext, "Info got Offline", Toast.LENGTH_SHORT).show()
        }

        val id: Int = intent.getIntExtra("ID", 1)
        //In the case we didn't find the file in Internal Storage, readFromFile returns the error
        //variable as a JSONObject. Then we either toast an error or fill info.
        if(info== JSONerror) Toast.makeText(applicationContext, error, Toast.LENGTH_LONG).show()
        else fillInfoUser(info, id)
    }

    override fun onStart(){
        super.onStart()

        accountID = findViewById(R.id.accountID)

        okBtn = findViewById(R.id.okBtn)
        okBtn.setOnClickListener {
            /*Depending on the return, we get the results on the mock API if online,
            //and we get the saved instance on the previous connection if offline.
            //If no connection was previously done, then nothing will be displayed
            val info = if(CheckConnection()){ secureConnect(FILE_NAME_ACCOUNT,"accounts") }
            else{ ReadFromFile(FILE_NAME_ACCOUNT, applicationContext) }*/

            lateinit var info: JSONObject
            val num = accountID.text.toString().toInt()
            if(num in 1..4) {
                try{
                    info=secureConnect(polyDecryption(FILE_NAME_ACCOUNT,mk),"accounts")
                    Toast.makeText(applicationContext, "Info got Online", Toast.LENGTH_SHORT).show()
                }
                catch(e: Exception){
                    info=readFromFile(polyDecryption(FILE_NAME_ACCOUNT,mk), applicationContext)
                    Toast.makeText(applicationContext, "Info got Offline", Toast.LENGTH_SHORT).show()
                }
                if(info== JSONerror) Toast.makeText(applicationContext, error, Toast.LENGTH_LONG).show()
                else fillInfoAccount(info, num)
            }
            else Toast.makeText(applicationContext, "No account for this ID number", Toast.LENGTH_SHORT).show()
        }

        /*refreshBtn = findViewById(R.id.refreshBtn)
        //The difference between this button and the ok button is that when we click on this one
        //we go get the info on the API if we are connected to the network, while with the ok button
        //we can get info from the API or from the internal storage of the phone if we are not
        //connected to any network.
        //When we click on the refresh button, we are inevitably connected to the network,
        //so we don't need to check the connected boolean and make an if condition
        refreshBtn.setOnClickListener {
            val info = secureConnect(FILE_NAME_ACCOUNT,"accounts")
            val num = accountID.text.toString().toInt()
            try{ fillInfoAccount(info, num)}
            catch (e: Exception){ e.printStackTrace() }
        }*/
    }

    private fun secureConnect(filename: String, ext: String) : JSONObject {
        lateinit var API_data : JSONObject
        //When calling an API through the network, process can be long and thus slowing the app.
        // Therefore, we need to create a thread to make the connection and get the data.
        //runBlocking is a sort of thread maker that converts usual callback-based code
        // to sequential code using coroutines
        runBlocking {
            val job = GlobalScope.async{
                lateinit var urlConnection: HttpsURLConnection
                try{
                    //First we create the URL object
                        val api = polyDecryption(API_KEY,mk)
                    val url = URL("https://$api.mockapi.io/labbbank/$ext")
                    //Open connection
                    urlConnection = url.openConnection() as HttpsURLConnection
                    /*
                    val keyStore: KeyStore = KeyStore.getInstance(KeyStore.getDefaultType())
                    val tmf = TrustManagerFactory.getInstance("X509", Security.getProviders()[0])
                    tmf.init(keyStore)
                    val context = SSLContext.getInstance("TLS")
                    context.init(null, tmf.getTrustManagers(), null)
                    urlConnection.setSSLSocketFactory(context.getSocketFactory())*/
                    var data = urlConnection.inputStream.bufferedReader().readText()
                    //We put data under a JSON format
                    data = "{ data :$data}"
                    //Now we can convert to JSON
                    API_data = JSONObject(data)
                    API_data
                }
                catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            API_data = job.await() as JSONObject
        }
        //Save info to file
        writeToFile(filename, API_data, applicationContext)
        return API_data
    }

    private fun writeToFile(filename: String, JsonObject: JSONObject, context: Context){
        try {
            // Convert JsonObject to String Format
            var jsonString: String = JsonObject.toString()
            // We encrypt the data before writing it to the file to add more security
            jsonString = polyEncryption(jsonString, mk)
            // Define the File Path and its Name
            val file = File(context.filesDir, filename)
            val bufferedWriter = BufferedWriter(FileWriter(file))
            bufferedWriter.write(jsonString)
            bufferedWriter.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
    }

    private fun readFromFile(filename: String, context: Context): JSONObject{
        lateinit var jsonObject : JSONObject
        //We add a try cvatch in case reading fail or if the file doesn't exist yet, meaning
        // the client tries to get info offline while he has never gotten them online.
        try {
            val file = File(context.filesDir, filename)
            val bufferedReader = BufferedReader(FileReader(file))
            val stringBuilder = StringBuilder()
            //Don't forget the ? because the string can be null, otherwise program throws an error
            var line: String? = bufferedReader.readLine()
            while (line != null) {
                stringBuilder.append(line)
                line = bufferedReader.readLine()
            }
            bufferedReader.close()
            //We need to decrypt the string with the same key we encrypted it with
            val jsonString = polyDecryption(stringBuilder.toString(), mk)
            // This response will have Json Format String, so we convert it to JSONObject
            jsonObject = JSONObject(jsonString)
        } catch (e: IOException) {
            jsonObject = JSONerror
            Log.e("Exception", "File read failed: $e")
            Toast.makeText(applicationContext, "No $filename", Toast.LENGTH_SHORT).show()
        }
        return jsonObject
    }

    private fun fillInfoUser(JsonObject: JSONObject, id: Int){
        val data : JSONObject = (JsonObject["data"] as JSONArray).getJSONObject(id - 1)
        val firstName = data.getString("name")
        val lastName = data.getString("lastname")
        name = findViewById(R.id.name)
        name.text = "$firstName $lastName"
    }

    private fun fillInfoAccount(JsonObject: JSONObject, id: Int){
        val data : JSONObject = (JsonObject["data"] as JSONArray).getJSONObject(id - 1)
        nameAccount = findViewById(R.id.accountValue)
        nameAccount.text = data.getString("accountName")
        amount = findViewById(R.id.amountValue)
        amount.text = data.getString("amount")
        iban = findViewById(R.id.ibanValue)
        iban.text = data.getString("iban")
        currency = findViewById(R.id.currencyValue)
        currency.text = data.getString("currency")
    }

    private fun polyEncryption(to_encrypt: String, key: String): String {
        val encrypted = StringBuilder()
        val size= key.length
        for(i in to_encrypt.indices){
            to_encrypt[i].toChar()
            var newAscii= to_encrypt[i].toInt() + key[i%size].toInt()
            if(newAscii>127) newAscii= 32 + newAscii%127
            encrypted.append(newAscii.toChar())
        }
        return encrypted.toString()
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

    /*This function was used to check connection at anytime meaning at the moment the device was
    not connected to any network, the application would know it thanks to this function, thus
    disabling the secure connect function, using the internal storage instead.
    fun CheckConnection(): Boolean{
        val connection = NetworkConnection(applicationContext)
        var con = true
        connection.observe(this, { isConnected ->
            con = if (isConnected) {
                Toast.makeText(applicationContext, "Online Mode", Toast.LENGTH_SHORT).show()
                true
            } else {
                Toast.makeText(applicationContext, "Offline Mode", Toast.LENGTH_SHORT).show()
                false
            }
        })
        return con
    }*/
}

