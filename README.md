# Clement_VALOT_IOS1_Mobile_Dev_Bankaccounts
# Create a secure application to see your bank accounts with Android Studio

## Explain how you ensure user is the right one starting the app
I have two activities/layouts in my application. On the first one (activity_main.xml), the user has to give the masterkey and a valid user_ID (between 1 and 73, we don't check user's credentials so any user can access any account at the moment he has the masterkey). The "Connect" button isn't enabled until both field have been filled and the user is redirected to the second activity/layout if and only if the masterkey is correct and the id is also valid. In case the user has one of the masterkey or the id wrong, a toast pops to tell him the inputs are wrong thus not indicating him if only the masterkey is incorrect or only the ID.

The masterkey isn't stored in brut text inside the source code. It is both hashed and hidden in another file. To hash the masterkey, I used the hashPassword function which uses Message.Digest to hash the string with SHA-256. Then, I encrypted this hash with the masterkey (in clear) with a polyEncryption function of my creation. And finally, I hid this encryption of this hash inside a C++ file. So when the user gives the masterkey, we compare the hashed value of his input with the decrypted value of our masterkey we get from the hidden file of our project. Like this, our masterkey can't be read by inspecting the source code, nor decrypted since the hash function can't be reversed engineered and the decryption function can't be cracked without the masterkey in clear itself. The only way to get pass this security would be extreme luck or if the masterkey was a classic password stored in a rainbow table (which it isn't since I hashed it and then tried to find it with CrackStation which didn't find any match -> "amps5r78sie").

The masterkey is the core element of the security of my application. Without it, not only the user can't access the second part of the application with all the information, but he cannot try to steal information from the source code since every important tokens (key, file name) and files themselves are encrypted with this particular key.


## How do you securely save user's data on your phone ?
In order to allow the offline mode, I need to be able to store the data retrieved from the API inside the user's phone so that when he doesn't have any connection, the application can simply check inside its memory to get the data (even if not up-to-date). To store data inside the device, Android Studio has multiple options: 
	Shared Preferences:  save primitive data in key-value pairs -> not interesting since we need to save JSON file
	External Storage or Shared Storage: Store files that your app intends to share with other apps, including media, documents, and other files. -> not interesting since we want 
to keep data for our application only.
	SQLite Database : not allowed.
	Internal Storage or App-specific Storage: same as external storage except files are meant for our app's use only -> our chosen option

Therefore, to store data inside files, we need to create them, write inside as well as read the information. So I created two functions WriteToFile and ReadFromFile that do the job. WriteToFile take as parameters the name of the file and the JSON object we extracted from the API, convert the JSON into one big string and write it inside the file. The file cannot be found inside the app or the root project but while running the emulator in "Device File Explorer -> data -> data -> com.example.bankaccounts -> files".And I have two files inside this directory: one for the users information and one for the accounts information. Then, where we are offline and we want to connect and get account info, we use the ReadFromFile function which takes as parameters the name of the file to read and returns a JSON object to fill the application with the corresponding data.

But information is not written in brut form inside those files, it is first encrypted with a function of my creation. polyDecryption is a simple function that change the ascii values of every characters of the string passed in parameters according to an encryption key (masterkey). To read those files, we simply use the polyDecryption function. 

Checking the connection of the client to the network wasn't an easy task. Because of deprecated methods, android studio forces us to use network callbacks which isn't intuitive. So I created another kotlin file NetworkConnection.kt to build the NetworkConnection class. Thanks to this, I can call it easily on my two other kotlin files linked two my two layouts to check whether the client is connected to the newtork or not with the implemented function of my class. When on the application, if the user turns off wifi and LTE, a toast pops to notify the offline mode. Furthermore, when offline, instead of calling the API, Connect and OK button read info from files in internal storage. However, even if this class is present in the source code, it is not used because it lead to some error while launching the app without any registered network. So I decided to keep it simple and only implement a try catch : we try the secureConnection, and if an error occurs (often meaning, client isn't connected to the network), then in the catch we call the function ReadFromFile retrieving info offline in Internal Storage.

Speaking about security, the connection with the API is made through the HTTPS protocol which uses TLS. This class also uses HostnameVerifier and SSLSocketFactory. 

## How did you hide the API url ?
In this project, I placed decoys : the encryption key in the C++ file is a false one and is never used; the build gradle file might indicate that I used build config to call the gradle.properties file in which I placed the API key and the hashed masterkey. While this is more secure than storing it in XML files, my tokens could still be decoded by someone via reverse engineering.

Native C/C++ code is harder to decompile and hence, hackers will have a harder time gaining access to my API keys. This has been proven to be more secure than storing it in gradle.properties file. CMake is a software tool that manages the build processes of other software. It stores keys securely, and give access to them through C++ file. 
1. First, I needed Native Development Kit (tool that is used to work with C/C++ code in Android), Low Level Debugger (debugger for native code), and CMake (tool that builds native C/C++ library).
2. Then I created a new folder, cpp, inside app/src/main, and did New → C/C++ Source File, and named my C++ file native-lib.cpp. I put in it:

#include <jni.h>  
#include <string>

extern "C" JNIEXPORT jstring

JNICALL
com_example_bankaccounts_accounts_getAPIKey(JNIEnv *env, jobject object) {
    std::string api_key = "my_api_key";
    return env->NewStringUTF(api_key.c_str());
}
* getAPIKey: This directly refers to the method name that I use in Kotlin later on.
* accounts: This refers to the Kotlin object in which I want to use the API key, where I'll interact with the C++ coded, and get a reference to my API key 
* com_example_bankaccounts: This refers to the package name corresponding to the Keys Kotlin object here. This should always point to the package of the class where I intend to use it. 

3. I created a CMakeLists.txt file. Under the app/ folder, create a new text file and name it CMakeLists.txt.
4. I didn't forget to configure Gradle for CMake
5. Now, I can call getAPIKey function in kotlin file

To hide the API url, I hid the API key in a C++ file, just like the encryption key, the masterkey and file names. All those tokens are also encrypted in the C++ file. API_KEY could then be called anywhere in the source code (in specified kotlin files) through getAPIkey() function. Plus, the last part of the url, i.e ":endpoint", is stored in a variable passed in parameters. This combined with code obfuscation makes it a little harder to get the API url. By the way, to activate code obfuscation, we simply put the minifyEnable to true in the gradle file.

## Screenshots of my Application
Since it is not practicle to put images in a readme and because I have a lot of them to show, i decided to create a pdf file named user experience where I explore all the cases and the reaction of my application to it. I put this file inside the git as well.
