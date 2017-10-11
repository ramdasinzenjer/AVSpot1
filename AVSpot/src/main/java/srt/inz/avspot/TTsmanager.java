package srt.inz.avspot;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

@SuppressWarnings("deprecation")
public class TTsmanager implements OnUtteranceCompletedListener {
	private TextToSpeech mTts = null;
    private boolean isLoaded = false;
    UtteranceProgressListener utl;

    public void init(Context context) {
        try {
            mTts = new TextToSpeech(context, onInitListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
      
    private TextToSpeech.OnInitListener onInitListener = new TextToSpeech.OnInitListener() {
        @SuppressLint("NewApi")
		@Override
        public void onInit(int status) {
        	
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTts.setLanguage(Locale.US);
                    isLoaded = true;
                    

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("error", "This Language is not supported");
                    }
                } else {
                    Log.e("error", "Initialization Failed!");
                }
                mTts.setOnUtteranceProgressListener(utl);
        }
        
    };
    

    public void shutDown() {
        mTts.stop();
    }

    public void addQueue(String text) {
        if (isLoaded)
            mTts.speak(text, TextToSpeech.QUEUE_ADD, null);
        else
            Log.e("error", "TTS Not Initialized");
    }

    public void initQueue(String text) {

        if (isLoaded)
            mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        else
            Log.e("error", "TTS Not Initialized");
    }

	@Override
	public void onUtteranceCompleted(String utteranceId) {
		// TODO Auto-generated method stub
		
		Log.i("LOG", "Speech completed");
	}
    
}


