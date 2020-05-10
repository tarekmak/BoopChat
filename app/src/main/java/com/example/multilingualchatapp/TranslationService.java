package com.example.multilingualchatapp;

import android.content.Context;
import android.content.res.Resources;
import android.os.StrictMode;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.ResourceBundle;

import androidx.annotation.NonNull;

//Singleton class of the TranslationService used to translate the messages
//(the reason why the singleton pattern was used is because we only need one instance of the
//TranslationService object to get the app to work properly)
class TranslationService {

    private String targLang;
    private static TranslationService translationInstance;// = new TranslationService();

    private Translate trans;

    private TranslationService() {
        String currentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserID).child("language").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    targLang = dataSnapshot.getValue().toString();
                    getTranslateService();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    //method used to process my google service account keys in re/raw/creds.json to get access to the
    //translation service
    private void getTranslateService() {

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("res/raw/creds.json")) {
            //Get credentials:
            final GoogleCredentials myCredentials = GoogleCredentials.fromStream(is);//GoogleCredentials.getApplicationDefault();

            //Set credentials and get translate service:
            TranslateOptions translateOptions = TranslateOptions.newBuilder().setCredentials(myCredentials).build();
            trans = translateOptions.getService();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    //method used to translate the string passed to the user's designated language and return the
    //translated string
    String translate(String org) {
        //detecting what's the language of the string passed to then translate to the user's designated language
        String detectRes = trans.detect(org).getLanguage();
        //checking if the detected language is either the same as the string we want to translate or undefined
        if (!detectRes.equals(targLang) && !("und").equals(detectRes)) {
            Translation translation = trans.translate(org, Translate.TranslateOption.sourceLanguage(detectRes), Translate.TranslateOption.targetLanguage(targLang), Translate.TranslateOption.model("base"));
            return translation.getTranslatedText();
        } else {
            return org;
        }
    }

    //method used to return the unique instance of the TranslationService object
    public static TranslationService getTranslationInstance() {
        if (translationInstance == null) {
            synchronized (TranslationService.class) {
                translationInstance = new TranslationService();
            }
        }
        return translationInstance;
    }
}
