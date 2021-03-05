# Clement_VALOT_IOS1_Mobile_Dev_Bankaccounts
Create a secure application to see your bank accounts with Android Studio

- Explain how you ensure user is the right one starting the app
I have two activities/layouts in my application. On the first one (activity_main.xml), the user has to give the masterkey and a valid user_ID (between 1 and 73, we don't check user's credentials so any user can access any account at the moment he has the masterkey). The "Connect" button isn't enabled until both field have been filled and the user is redirected to the second activity/layout if and only if the masterkey is correct and the id is also valid. In case the user has one of the masterkey or the id wrong, a toast pops to tell him the inputs are wrong thus not indicating him if only the masterkey is incorrect or only the ID.

The masterkey isn't stored in brut text inside the source code, it is both hashed and hidden in another file. To hash the masterkey, I used the hashPassword function which uses Message.Digest to hash the string with SHA-256. So when the user gives the masterkey, we compare the hashed value of his input with the value of our masterkey we get from the hidden file of our project. Like this, our masterkey can't be read by inspecting the code, nor decrypted since the hash function can't be reversed engineered. The only way to get pass this security would be extreme luck or if the masterkey was a classic password stored in a rainbow table (which it is -> Masterkey).


- How do you securely save user's data on your phone ?
In order to allow the offline mode, I need to be able to store the data retrieved from the API inside the user's phone so that when he doesn't have any connection, the application
can simply check inside its memory to get the data (even if not up-to-date). To store data inside the device, Android Studio has multiple options: 
	Shared Preferences:  save primitive data in key-value pairs -> not interesting since we need to save JSON file
	External Storage or Shared Storage: Store files that your app intends to share with other apps, including media, documents, and other files. -> not interesting since we want 
to keep data for our application only.
	SQLite Database : not allowed.
	Internal Storage or App-specific Storage: same as external storage except files are meant for our app's use only -> our chosen option

Therefore, to store data inside files, we need to create them, write inside as well as read the information. So I created two functions WriteToFile and ReadFromFile that do the job.
WriteToFile take as parameters the name of the file and the JSON object we extracted from the API, convert the JSON into one big string and write it inside the file. The file cannot be found inside the app or the root project but while running the emulator in "Device File Explorer -> data -> data -> com.example.bankaccounts -> files".And I have two files inside this directory: one for the users information and one for the accounts information. Then, where we are offline and we want to connect and get account info, we use the ReadFromFile function which takes as parameters the name of the file to read and returns a JSON object to fill the application with the corresponding data.

But information is not written in brut form inside those files, it is first encrypted with a function of my creation. polyDecryption is a simple function that change the ascii values of every characters of the string passed in parameters according to an encryption key. This encryption key is itself hidden in another file. To read those files, we simply use the polyDecryption function. 

Checking the connection of the client to the network wasn't an easy task. Because of deprecated methods, android studio forces us to use network callbacks which isn't intuitive.
So I created another kotlin file NetworkConnection.kt to build the NetworkConnection class. Thanks to this, I can call it easily on my two other kotlin files linked two my two layouts to check wether the client is connected to the newtork or not with the implemented function of my class. When on the application, if the user turns off wifi and LTE, a toast pops to notify the offline mode. Furthermore, when offline, instead of calling the API, Connect and OK button read info from files in internal storage, and the refresh button for the 
activiy_accounts.xml gets disable. However, even if this class is present in the source code, it is not used because it led to some error while launching the app without any registered network. So I decided to keep it simple and only implement a try catch : we try the secureConnection, and if an error occurs (often meaning, client isn't connected to the network), then in the catch we call the function ReadFromFile retrieving info offline in Internal Storage.

Speaking about security, the connection with the API is made through the HTTPS protocol which uses TLS.

- How did you hide the API url ?
To hide the API url, I hid it in a .cpp file like the encryption key and the masterkey. API_KEY could then be called anywhere in the source code through getAPIkey() function. Plus, the last part of the url, i.e ":endpoint", is stored in a variable passed in parameters. This combined with code obfuscation makes it a little harder to get the API url. By the way, to activate code obfuscation, we simply put the minifyEnable to true in the gradle file.

