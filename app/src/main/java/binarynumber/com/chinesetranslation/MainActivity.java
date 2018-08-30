package binarynumber.com.chinesetranslation;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.naver.speech.clientapi.SpeechRecognitionResult;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import binarynumber.com.chinesetranslation.utils.AudioWriterPCM;

public class MainActivity extends Activity {
    //
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String CLIENT_ID = "YOUR_KEY";
    // 1. "내 애플리케이션"에서 Client ID를 확인해서 이곳에 적어주세요.
    // 2. build.gradle (Module:app)에서 패키지명을 실제 개발자센터 애플리케이션 설정의 '안드로이드 앱 패키지 이름'으로 바꿔 주세요
    private RecognitionHandler handler;
    private NaverRecognizer naverRecognizer;
    private String mResult;
    private AudioWriterPCM writer;
    //
    Button btKC, btCK, btTr;
    EditText etSource;
    TextView tvResult, tvStatus;
    String sourceLang, targetLang, strName;
    List<String> results;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean isGrant = grantPermission();
        if(isGrant) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            etSource = (EditText) findViewById(R.id.et_source);
            tvResult = (TextView) findViewById(R.id.tv_result);
            tvStatus = (TextView) findViewById(R.id.status);
            btKC = (Button) findViewById(R.id.bt_KC);
            btCK = (Button) findViewById(R.id.bt_CK);
            btTr = (Button) findViewById(R.id.bt_translation);
            handler = new RecognitionHandler(this);
            naverRecognizer = new NaverRecognizer(this, handler, CLIENT_ID);

            //한->중
            btKC.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!naverRecognizer.getSpeechRecognizer().isRunning()) {
                        // Start button is pushed when SpeechRecognizer's state is inactive.
                        // Run SpeechRecongizer by calling recognize().
                        mResult = "";
                        findViewById(R.id.tv_Record).setVisibility(View.VISIBLE);
                        tvStatus.setText("Connecting...");
                        tvStatus.setText(R.string.str_stop);
                        sourceLang = "ko";
                        targetLang = "zh-CN";
                        naverRecognizer.recognizeKC();
                    } else {
                        Log.d(TAG, "stop and wait Final Result");
                        Toast.makeText(MainActivity.this,"stop and wait Final Result",Toast.LENGTH_SHORT).show();
                        btKC.setEnabled(false);
                        naverRecognizer.getSpeechRecognizer().stop();
                    }
                }
            });

            //중->한
            btCK.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!naverRecognizer.getSpeechRecognizer().isRunning()) {
                        // Start button is pushed when SpeechRecognizer's state is inactive.
                        // Run SpeechRecongizer by calling recognize().
                        mResult = "";
                        findViewById(R.id.tv_Record).setVisibility(View.VISIBLE);
                        tvStatus.setText("Connecting...");
                        tvStatus.setText(R.string.str_stop);
                        sourceLang = "zh-CN";
                        targetLang = "ko";
                        naverRecognizer.recognizeCK();
                    } else {
                        Log.d(TAG, "stop and wait Final Result");
                        btKC.setEnabled(false);
                        naverRecognizer.getSpeechRecognizer().stop();
                    }
                }
            });

            //수정 번역 엔터키 리스너
            etSource.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View view, int i, KeyEvent keyEvent) {
                    //Enter key Action
                    if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) && (i == KeyEvent.KEYCODE_ENTER)) {
                        //Enter키눌렀을떄 처리
                        NaverTranslateTask asyncTask = new NaverTranslateTask();
                        String sText = etSource.getText().toString();
                        asyncTask.execute(sText);
                        return true;
                    }
                    return false;
                }
            });
            btTr.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(targetLang != null && sourceLang != null){
                        NaverTranslateTask asyncTask = new NaverTranslateTask();
                        String sText = etSource.getText().toString();
                        asyncTask.execute(sText);
                    }else Toast.makeText(MainActivity.this,"음성인식 먼저 실행해주세요!",Toast.LENGTH_SHORT).show();
                }
            });

            //음성 인식 결과 후보군 선택
            etSource.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if(results != null && !results.isEmpty()){
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(
                            MainActivity.this);
                    alertBuilder.setIcon(R.drawable.check);
                    alertBuilder.setTitle("항목중에 하나를 선택하세요.");
                    // List Adapter 생성
                    final ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            MainActivity.this,
                            android.R.layout.select_dialog_singlechoice, results);
                   // adapter.add("직접입력");
                    // 버튼 생성
                        alertBuilder.setNeutralButton("직접입력", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
                                ad.setTitle("직접입력");       // 제목 설정
                                ad.setMessage("텍스트를 입력해주세요.");   // 내용 설정
                                // EditText 삽입하기
                                final EditText et = new EditText(MainActivity.this);
                                ad.setView(et);
                                // 확인 버튼 설정
                                ad.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        strName = et.getText().toString();
                                        dialog.dismiss();     //닫기
                                    }});
                                // 취소 버튼 설정
                                ad.setNegativeButton("닫기", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();     //닫기
                                    }
                                });
                            }
                        });
                        alertBuilder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }});
                        alertBuilder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                strName = adapter.getItem(i);
                                etSource.setText(strName);
                                NaverTranslateTask asyncTask = new NaverTranslateTask();
                                String sText = etSource.getText().toString();
                                asyncTask.execute(sText);
                            }
                        });
                        alertBuilder.show();
                    }
                    else Toast.makeText(MainActivity.this,"음성인식 먼저 실행하세요!",Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        }
    }

    //ASYNCTASK
    public class NaverTranslateTask extends AsyncTask<String, Void, String> {
        //Naver
        String clientId = "YOUR_KEY";//애플리케이션 클라이언트 아이디값";
        String clientSecret = "YOUR_KEY";//애플리케이션 클라이언트 시크릿값";
        //언어선택도 나중에 사용자가 선택할 수 있게 옵션 처리해 주면 된다.

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        //AsyncTask 메인처리
        @Override
        protected String doInBackground(String... strings) {
            String sourceText = strings[0];
            try {
                String text = URLEncoder.encode(sourceText, "UTF-8");
                String apiURL = "https://openapi.naver.com/v1/papago/n2mt";
                URL url = new URL(apiURL);
                HttpURLConnection con = (HttpURLConnection)url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("X-Naver-Client-Id", clientId);
                con.setRequestProperty("X-Naver-Client-Secret", clientSecret);
                // post request
                String postParams = "source="+sourceLang+"&target="+targetLang+"&text=" + text;
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(postParams);
                wr.flush();
                wr.close();
                int responseCode = con.getResponseCode();
                BufferedReader br;
                if(responseCode==200) { // 정상 호출
                    br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                } else {  // 에러 발생
                    br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                }
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = br.readLine()) != null) {
                    response.append(inputLine);
                }
                br.close();
                //System.out.println(response.toString());
                return response.toString();

            } catch (Exception e) {
                //System.out.println(e);
                Log.d("error", e.getMessage());
                return null;
            }
        }

        //번역된 결과를 받아서 처리
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //JSON데이터를 자바객체로 변환해야 한다.
            //Gson을 사용할 것이다.
            Gson gson = new GsonBuilder().create();
            JsonParser parser = new JsonParser();
            JsonElement rootObj = parser.parse(s.toString())
                    //원하는 데이터 까지 찾아 들어간다.
                    .getAsJsonObject().get("message")
                    .getAsJsonObject().get("result");
            //안드로이드 객체에 담기
            TranslatedItem items = gson.fromJson(rootObj.toString(), TranslatedItem.class);
            //Log.d("result", items.getTranslatedText());
            //번역결과를 텍스트뷰에 넣는다.
            tvResult.setText(items.getTranslatedText());
        }

        //자바용 그릇
        private class TranslatedItem {
            String translatedText;

            public String getTranslatedText() {
                return translatedText;
            }
        }
    }

    // Declare handler for handling SpeechRecognizer thread's Messages.
    static class RecognitionHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        RecognitionHandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                activity.handleMessage(msg);
            }
        }
    }
    // Handle speech recognition Messages.
    private void handleMessage(Message msg) {
        switch (msg.what) {
            case R.id.clientReady:
                // Now an user can speak.
                tvStatus.setText("Connected");
                writer = new AudioWriterPCM(
                        Environment.getExternalStorageDirectory().getAbsolutePath() + "/NaverSpeechTest");
                writer.open("Test");
                break;

            case R.id.audioRecording:
                writer.write((short[]) msg.obj);
                break;

            case R.id.partialResult:
                // Extract obj property typed with String.
                mResult = (String) (msg.obj);
                etSource.setText(mResult);
                break;

            case R.id.finalResult:
                // Extract obj property typed with String array.
                // The first element is recognition result for speech.
                findViewById(R.id.tv_Record).setVisibility(View.INVISIBLE);
                SpeechRecognitionResult speechRecognitionResult = (SpeechRecognitionResult) msg.obj;
                results = speechRecognitionResult.getResults();

                mResult = results.get(0);
                etSource.setText(mResult);
                //번역
                NaverTranslateTask asyncTask = new NaverTranslateTask();
                String sText = etSource.getText().toString();
                asyncTask.execute(sText);
                //
                break;

            case R.id.recognitionError:
                if (writer != null) {
                    writer.close();
                }

                mResult = "Error code : " + msg.obj.toString();
                tvStatus.setText(mResult);
                //tvStatus.setText(R.string.str_start);
                btKC.setEnabled(true);
                break;

            case R.id.clientInactive:
                if (writer != null) {
                    writer.close();
                }

                tvStatus.setText(R.string.str_start);
                btKC.setEnabled(true);
                break;
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        // NOTE : initialize() must be called on start time.
        naverRecognizer.getSpeechRecognizer().initialize();
    }
    @Override
    protected void onResume() {
        super.onResume();

        mResult = "";
        etSource.setText("");
        tvStatus.setText("");
        btKC.setEnabled(true);
    }
    @Override
    protected void onStop() {
        super.onStop();
        // NOTE : release() must be called on stop time.
        naverRecognizer.getSpeechRecognizer().release();
    }

    //권한
    private boolean grantPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted");
                return true;
            }else{
                Log.v(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO}, 1);
                return false;
            }
        }else{
            Toast.makeText(this, "Permission is Grant", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Permission is Grant ");
            return true;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (Build.VERSION.SDK_INT >= 23) {
            if(grantResults[0]== PackageManager.PERMISSION_GRANTED)
                Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
            if(grantResults[1]== PackageManager.PERMISSION_GRANTED)
                Log.v(TAG,"Permission: "+permissions[1]+ "was "+grantResults[1]);
        }
    }
}
