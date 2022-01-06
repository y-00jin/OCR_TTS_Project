package com.example.ocr_tts_project;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.IOException;
import java.util.Locale;

import static android.speech.tts.TextToSpeech.ERROR;

public class OCR_TTS_Activity extends AppCompatActivity {


    private TextToSpeech tts;   //tts 변수 선언
    Button button_capture, button_tts;
    TextView textview_data;
    Bitmap bitmap;
    private static final int REQUEST_CAMERA_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ocr_tts_main);

        button_capture = findViewById(R.id.button_capture); // button_capture이란 아이디를 가진 버튼을 가져옴
        button_tts = findViewById(R.id.button_tts); // button_tts이란 아이디를 가진 버튼을 가져옴
        textview_data = findViewById(R.id.text_data);   //text_data란 아이디를 가진 텍스트뷰를 가져옴

        if(ContextCompat.checkSelfPermission(OCR_TTS_Activity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){  // 카메라 권한이 부여되지 않았으면 권한요청 처리
            ActivityCompat.requestPermissions(OCR_TTS_Activity.this, new String[]{
                    Manifest.permission.CAMERA  // 부여할 권한(카메라)
            },REQUEST_CAMERA_CODE);
        }

        button_capture.setOnClickListener(new View.OnClickListener(){   // 촬영버튼 클릭 리스너 (카메라를 실행하고 캡쳐한 이미지를 잘라내는 작업 -> 이미지를 잘란내는 작업은 라이브러리 사용함)

            @Override
            public void onClick(View v) {
                // 이미지 자르는 기능
                CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(OCR_TTS_Activity.this);   // 이미지 자르기를 시작하고 카메라의 이미지를 클릭하도록 요청(아니면 갤러리에 있는 사진을 사용자가 자르도록)
            }
        });


        // TTS를 생성하고 OnInitListener로 초기화 한다.
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != ERROR) {
                    // 언어를 선택한다.
                    tts.setLanguage(Locale.KOREAN);
                }
            }
        });

        button_tts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tts.speak(textview_data.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);   // textView에 있는 데이터를 tts객체를 이용해 음성 출력
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){  //요청한 코드가 이미지 자르는 거 일때
            // 자른 이미지를 가져와야함
            CropImage.ActivityResult result = CropImage.getActivityResult(data);    // 자른이미지 데이터를 result에 저장
            if(resultCode == RESULT_OK){
                Uri resultUri = result.getUri();    // 얻은 결과로 uri 생성
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);   // 위에 생성한 uri로 비트맵 객체 생성
                    getTextFromImage(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private void getTextFromImage(Bitmap bitmap){
        //
        TextRecognizer recognizer = new TextRecognizer.Builder(this).build();   // 텍스트를 인식할 객체 생성
        if(!recognizer.isOperational()){    // 텍스트 인식이 제대로 작동하지 않았을경우
            Toast.makeText(OCR_TTS_Activity.this,"오류 발생", Toast.LENGTH_SHORT).show();   // 오류 메시지 발생
        }else{  //제대로 작동했을 경우 텍스트 추출
            Frame frame = new Frame.Builder().setBitmap(bitmap).build();    // 가져온 비트맵을 전달
            SparseArray<TextBlock> textBlockSparseArray = recognizer.detect(frame); //텍스트블록에 얻은 프레임 전달
            StringBuilder stringBuilder = new StringBuilder();  // 이미지에서 얻은 문자를 저장할 stringBuilder 생성
            for(int i=0;i<textBlockSparseArray.size();i++){
                TextBlock textBlock = textBlockSparseArray.valueAt(i);  // 하나씩 텍스트를 얻음
                stringBuilder.append(textBlock.getValue()); //stringBuilder에 하나씩 추가
                stringBuilder.append("\n"); // 한줄 유지
            }
            textview_data.setText(stringBuilder.toString());    // 얻은 문자열을 텍스트뷰 데이터에 추가
            button_capture.setText("재촬영");  // 버튼 이름 변경
            button_tts.setVisibility(View.VISIBLE); //버튼이 보이게 설정
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TTS 객체가 남아있다면 실행을 중지하고 메모리에서 제거한다.
        if(tts != null){
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }
}
